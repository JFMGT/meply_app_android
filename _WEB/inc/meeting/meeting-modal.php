<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, esc, getProfileDocumentId etc.

$eventIdRaw = $_GET['eventID'] ?? '';
$eventStart = null;
$eventEnd = null;
$eventFixed = false;

// --- 1. API-Aufruf (mit Fixes) ---
// SICHERHEIT: Explizite Login-Prüfung statt nur !empty()
if (!empty($eventIdRaw) && is_logged_in_explizit()) { 
    $jwt = $_SESSION['jwt'];
    
    // SICHERHEIT (Path Injection): $eventIdRaw MUSS urlencoded werden
    $apiUrl =  STRAPI_API_BASE . "/events/" . urlencode($eventIdRaw);
    debugLog("Get Event (for Modal) URL: " . $apiUrl);

    // ROBUSTHEIT: @file_get_contents durch cURL ersetzt
    $ch = curl_init($apiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $jwt",
        "Accept: application/json"
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    // ROBUSTHEIT: Fehlerbehandlung
    if (!$curlError && $httpCode === 200 && $response) {
        $eventData = json_decode($response, true);
        $attributes = $eventData['data'] ?? [];

        $eventFixed = $attributes['fixed_date'] ?? false;

        // ROBUSTHEIT: new DateTime() in try...catch
        if ($eventFixed && !empty($attributes['start_date'])) {
            try {
                $eventStart = new DateTime($attributes['start_date']);
                $eventEnd = !empty($attributes['end_date'])
                            ? new DateTime($attributes['end_date']) 
                            : clone $eventStart;
            } catch (Exception $e) {
                // Fehler beim Parsen des Datums von der API
                debugLog("Event Modal Date Parse Error: " . $e->getMessage());
                $eventStart = null; // Zurücksetzen, um Fehler im HTML zu vermeiden
                $eventFixed = false;
            }
        }
    } else {
         debugLog("Event Modal API Error: HTTP {$httpCode}, cURL: {$curlError}");
         // Kein exit, Modal wird einfach als Standard (ohne Event-Daten) gerendert
    }
}
?>

<div id="createMeetingModal" class="modal hidden">
  <div class="modal-content">
    <span class="close">&times;</span>
    <h2 id="modalTitle">Gesuch erstellen</h2>
    <form id="meetingForm">
      
      <input type="hidden" name="locationId" id="locationId">
      <input type="hidden" name="eventId" id="eventId" value="<?php echo esc($eventIdRaw); ?>">
      <input type="hidden" name="meetingId" id="meetingId">

      <label for="title">Titel</label>
      <input type="text" id="title" name="title" required>

      <label for="description">Beschreibung</label>
      <textarea id="description" name="description"></textarea>

      <?php if ($eventIdRaw && !$eventFixed): ?>
        <p><em>Dieses Event ist noch nicht terminiert. Eine Datumsauswahl ist aktuell nicht möglich.</em></p>
      
      <?php elseif ($eventStart): // Nur wenn $eventStart erfolgreich gesetzt wurde ?>
        <label>Verfügbare Tage beim Event:</label>
        <div id="eventDaysSelector">
          <?php
            if ($eventEnd && $eventEnd >= $eventStart) {
                $interval   = new DateInterval('P1D');
                $endPlusOne = (clone $eventEnd)->modify('+1 day');
                $period     = new DatePeriod($eventStart, $interval, $endPlusOne);

                foreach ($period as $date) {
                    $value = $date->format('Y-m-d');
                    $label = $date->format('d.m.Y');
                    // SICHERHEIT (XSS-FIX): $value im Attribut escapen
                    echo "<label><input type='checkbox' name='eventDays[]' value='" . esc($value) . "'> " . esc($label) . "</label><br>";
                }
            } else {
                $value = $eventStart->format('Y-m-d');
                $label = $eventStart->format('d.m.Y');
                // SICHERHEIT (XSS-FIX): $value im Attribut escapen
                echo "<label><input type='checkbox' name='eventDays[]' value='" . esc($value) . "'> " . esc($label) . "</label><br>";
            }
          ?>
        </div>
      <?php else: ?>
        <!-- ✅ FIX: Datumsfelder hinzugefügt -->
        <label>Wann soll gespielt werden?</label>
        <select id="dateType" name="dateType">
          <option value="fixed">Spezifisches Datum</option>
          <option value="range">Zeitraum</option>
          <option value="recurring">Wiederkehrend</option>
        </select>

        <!-- ✅ Container für spezifisches Datum -->
        <div id="dateFixed" class="date-option hidden">
          <label for="date">Datum und Uhrzeit</label>
          <input type="datetime-local" id="date" name="date">
        </div>

        <!-- ✅ Container für Zeitraum -->
        <div id="dateRange" class="date-option hidden">
          <label for="dateFrom">Von</label>
          <input type="date" id="dateFrom" name="dateFrom">
          
          <label for="dateTo">Bis</label>
          <input type="date" id="dateTo" name="dateTo">
        </div>

        <!-- ✅ Container für wiederkehrende Termine -->
        <div id="dateRecurring" class="date-option hidden">
          <label>Wochentage</label>
          <div>
            <label><input type="checkbox" name="recurringDays[]" value="monday"> Montag</label><br>
            <label><input type="checkbox" name="recurringDays[]" value="tuesday"> Dienstag</label><br>
            <label><input type="checkbox" name="recurringDays[]" value="wednesday"> Mittwoch</label><br>
            <label><input type="checkbox" name="recurringDays[]" value="thursday"> Donnerstag</label><br>
            <label><input type="checkbox" name="recurringDays[]" value="friday"> Freitag</label><br>
            <label><input type="checkbox" name="recurringDays[]" value="saturday"> Samstag</label><br>
            <label><input type="checkbox" name="recurringDays[]" value="sunday"> Sonntag</label>
          </div>

          <label for="recurringFrequency">Frequenz</label>
          <select id="recurringFrequency" name="recurringFrequency">
            <option value="weekly">Wöchentlich</option>
            <option value="biweekly">Alle 2 Wochen</option>
            <option value="monthly">Monatlich</option>
          </select>
        </div>
      <?php endif; ?>

      <button type="submit">Speichern</button>
    </form>
  </div>
</div>