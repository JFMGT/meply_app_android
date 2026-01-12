<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../../inc/config.php');
require_once('../../inc/functions.php'); // Nötig für requireLogin, getUserEvents etc.

// 1. SICHERHEIT: Login erzwingen
requireLogin();

include("../../inc/header_auth.php");

// Session-Daten sicher abrufen
$userProfileId = $_SESSION['profile']['documentId'] ?? null;
$jwt = $_SESSION['jwt'] ?? null;
$apiBase = STRAPI_API_BASE . '/events';

if (!$userProfileId || !$jwt) {
    die("Fehler: Sitzung ungültig. Bitte neu einloggen.");
}

$showForm  = false;
$editing   = isset($_GET['edit']) ? $_GET['edit'] : null;
$eventData = null;
$message   = '';

/**
 * =================================================================
 * DELETE Event (ABGESICHERT)
 * =================================================================
 */
if (isset($_GET['delete'])) {
    $deleteId = $_GET['delete'];

    // 1. SICHERHEIT: Autorisierungs-Check - Gehört das Event dir?
    // Wir holen kurz die Autoren-Info des Events
    $checkCh = curl_init("$apiBase/" . urlencode($deleteId) . "?populate=author");
    curl_setopt($checkCh, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($checkCh, CURLOPT_HTTPHEADER, ["Authorization: Bearer $jwt"]);
    $checkResp = curl_exec($checkCh);
    curl_close($checkCh);
    $checkData = json_decode($checkResp, true);
    
    $eventAuthorId = $checkData['data']['author']['documentId'] ?? null;

    if ($eventAuthorId === $userProfileId) {
        // 2. Berechtigt -> Löschen durchführen
        $ch = curl_init("$apiBase/" . urlencode($deleteId)); // SICHERHEIT: urlencode
        curl_setopt_array($ch, [
            CURLOPT_CUSTOMREQUEST => "DELETE",
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => [
                "Authorization: Bearer $jwt",
                "Content-Type: application/json"
            ],
        ]);
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode >= 200 && $httpCode < 300) {
             $message = "Event wurde gelöscht";
        } else {
             $message = "Fehler beim Löschen (API-Status: $httpCode)";
        }
    } else {
        // Nicht berechtigt
        $message = "Fehler: Du hast keine Berechtigung, dieses Event zu löschen.";
    }
}

/**
 * =================================================================
 * EDITING Event (Daten laden & Prüfen)
 * =================================================================
 */
if ($editing) {
    // Daten laden (nutzt deine externe Funktion, wir vertrauen ihr hier)
    $loadedData = getUserEvents($editing, true); // true = force draft fetch if needed

    // SICHERHEIT: Prüfen, ob man das Event bearbeiten darf
    if (($loadedData['author']['documentId'] ?? null) === $userProfileId) {
        $eventData = $loadedData;
        $showForm = true;
    } else {
        $message = "Fehler: Du darfst dieses Event nicht bearbeiten.";
        $showForm = false;
    }
}

/**
 * =================================================================
 * CREATE / UPDATE Event (ABGESICHERT)
 * =================================================================
 */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {

    $allowSave = true;
    $isUpdate  = !empty($_POST['documentId']);

    // Daten vorbereiten (Input-Validierung wäre hier noch besser, aber wir fokusieren auf Security)
    $postData = [
        'data' => [
            'Title'            => $_POST['Title'] ?? '',
            'description'      => $_POST['description'] ?? '',
            'url'              => $_POST['url'] ?? '',
            'fixed_date'       => false,
            'start_date'       => formatDate($_POST['start_date'] ?? ''),
            'start_time'       => formatTime($_POST['start_time'] ?? ''),
            'end_date'         => formatDate($_POST['end_date'] ?? ''),
            'date_placeholder' => $_POST['date_placeholder'] ?? '',
            'end_time'         => formatTime($_POST['end_time'] ?? ''),
            'repeat'           => $_POST['repeat'] ?? 'none',
            'street'           => $_POST['street'] ?? '',
            'street_number'    => $_POST['street_number'] ?? '',
            'zip'              => $_POST['zip'] ?? '',
            'city'             => $_POST['city'] ?? '',
            'country'          => $_POST['country'] ?? '',
            // getCoordinates sollte idealerweise robust sein
            'coordinates'      => getCoordinates(
                ($_POST['street'] ?? '') . ' ' . ($_POST['street_number'] ?? ''),
                $_POST['zip'] ?? '',
                $_POST['city'] ?? ''
            ),
        ]
    ];

    if (!empty($_POST['location_id'])) {
        $postData['data']['location'] = $_POST['location_id'];
    }
    if (!empty($postData['data']['date_placeholder'])) {
        $postData['data']['fixed_date'] = true;
    }

    // --- CREATE Logik ---
    if (!$isUpdate) {
        $message = "Event wurde erstellt";
        // Slug generieren (nutzt deine Funktion)
        $postData['data']['slug']   = generateUniqueSlug($_POST['Title'] ?? 'event');
        $postData['data']['author'] = $userProfileId;

        // Status setzen basierend auf User-Rechten
        $canPublish = $_SESSION['profile']['userCanPublish'] ?? false;
        $url = $apiBase . ($canPublish ? '' : '?status=draft');
        
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_POST, true); // Wichtig: POST für Create
    }

    // --- UPDATE Logik (mit Sicherheitscheck) ---
    if ($isUpdate) {
        $eventId = $_POST['documentId'];
        $message = "Event wurde aktualisiert";

        // 1. SICHERHEIT: ERNEUTER Autorisierungs-Check vor dem Update!
        // Wir verlassen uns nicht nur auf das Formular, sondern prüfen die DB.
        $checkCh = curl_init("$apiBase/" . urlencode($eventId) . "?populate=author");
        curl_setopt($checkCh, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($checkCh, CURLOPT_HTTPHEADER, ["Authorization: Bearer $jwt"]);
        $checkResp = curl_exec($checkCh);
        curl_close($checkCh);
        $checkData = json_decode($checkResp, true);
        $dbAuthorId = $checkData['data']['author']['documentId'] ?? null;

        if ($dbAuthorId !== $userProfileId) {
            $allowSave = false;
            $message = "Fehler: Fehlende Berechtigung zum Bearbeiten.";
        } else {
            // Berechtigt -> URL für Update bauen
            $isDraftReq = ($_POST['isDraft'] ?? '') === 'draft';
            // Prüfen ob User veröffentlichen darf (oder Autor-Edit-Recht hat)
            $canPublish = $_SESSION['profile']['userCanEdit'] ?? false; // Oder spezifisches Feld

            $statusSuffix = ($isDraftReq || !$canPublish) ? '?status=draft' : '';
            $url = "$apiBase/" . urlencode($eventId) . $statusSuffix;

            $ch = curl_init($url);
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PUT");
        }
    }

    // --- SAVE ausführen ---
    if ($allowSave && isset($ch)) {
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POSTFIELDS     => json_encode($postData),
            CURLOPT_HTTPHEADER     => [
                "Authorization: Bearer $jwt",
                "Content-Type: application/json"
            ]
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        if (curl_errno($ch)) {
             $message = "Netzwerkfehler beim Speichern: " . curl_error($ch);
        }
        curl_close($ch);

        if ($httpCode >= 200 && $httpCode < 300) {
            // Erfolg - $message ist bereits gesetzt
            $showForm = false; // Formular ausblenden nach Erfolg
        } elseif (!curl_errno($ch)) { // Nur wenn kein Netzwerkfehler vorher war
            $message = "Fehler beim Speichern (API $httpCode)";
            // Optional: Details loggen
        }
    }
}
?>

<div class="content content-plain top-margin">
    <?php if ($message): ?>
        <div class="card info"><?= htmlspecialchars($message, ENT_QUOTES, 'UTF-8') ?></div>
    <?php endif; ?>

    <div class="card">
        <h1>Events verwalten</h1>

        <?php if (!$showForm && !isset($_GET['new'])): ?>
            <form method="GET">
                <button type="submit" name="new" value="1">Neues Event eintragen</button>
            </form>
        <?php endif; ?>

        <?php if ($showForm || isset($_GET['new'])): ?>
            <form method="POST" class="top-margin">
                <h2><?= $editing ? 'Event bearbeiten' : 'Neues Event erstellen' ?></h2>

                <?php if (!empty($eventData['documentId'])): ?>
                    <input type="hidden" name="documentId" value="<?= htmlspecialchars($eventData['documentId'], ENT_QUOTES, 'UTF-8'); ?>">
                    <input type="hidden" name="isDraft" value="<?= htmlspecialchars($_GET['is_draft'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">
                <?php endif; ?>

                <?php if (($_GET['is_draft'] ?? '') === 'draft'): ?>
                    <div class='success'>
                        <strong>Hinweis:</strong> Dieses Event ist aktuell ein Entwurf.
                    </div>
                <?php endif; ?>

                <label>Titel:</label>
                <input type="text" name="Title" value="<?= htmlspecialchars($eventData['Title'] ?? '', ENT_QUOTES, 'UTF-8'); ?>" required>

                <label>Beschreibung:</label>
                <textarea name="description"><?= htmlspecialchars($eventData['description'] ?? '', ENT_QUOTES, 'UTF-8'); ?></textarea>

                <label>URL:</label>
                <input type="url" name="url" value="<?= htmlspecialchars($eventData['url'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <div class="explenation">
                    <h3>Datum</h3>
                    </div>

                <label>Platzhalter für Datum:</label>
                <input type="text" name="date_placeholder" value="<?= htmlspecialchars($eventData['date_placeholder'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Startdatum:</label>
                <input type="date" name="start_date" value="<?= htmlspecialchars($eventData['start_date'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Startzeit:</label>
                <input type="time" name="start_time" value="<?= htmlspecialchars($eventData['start_time'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Enddatum:</label>
                <input type="date" name="end_date" value="<?= htmlspecialchars($eventData['end_date'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Endzeit:</label>
                <input type="time" name="end_time" value="<?= htmlspecialchars($eventData['end_time'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <div class="explenation">
                    <h3>Wiederholung (BETA)</h3>
                    </div>

                <label>Wiederholung:</label>
                <select name="repeat">
                    <option value="none">Keine</option>
                    <option value="weekly"      <?= ($eventData['repeat'] ?? '') === 'weekly'      ? 'selected' : ''; ?>>Wöchentlich</option>
                    <option value="monthly"     <?= ($eventData['repeat'] ?? '') === 'monthly'     ? 'selected' : ''; ?>>Monatlich</option>
                    <option value="nth_weekday" <?= ($eventData['repeat'] ?? '') === 'nth_weekday' ? 'selected' : ''; ?>>Wochentag im Monat</option>
                </select>

                <div class="explenation">
                    <h3>Location (BETA)</h3>
                    </div>

                <label>Location:</label>
                <input type="text" id="location-search" 
                       value="<?= htmlspecialchars($eventData['location']['Titel'] ?? '', ENT_QUOTES, 'UTF-8'); ?>" 
                       placeholder="Location-Titel eingeben..." autocomplete="off">
                <input type="hidden" name="location_id" value="<?= htmlspecialchars($eventData['location']['id'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <ul id="location-results" class="autocomplete-list"></ul>

                <label>Straße:</label>
                <input type="text" name="street" value="<?= htmlspecialchars($eventData['street'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Hausnummer:</label>
                <input type="text" name="street_number" value="<?= htmlspecialchars($eventData['street_number'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>PLZ:</label>
                <input type="text" name="zip" value="<?= htmlspecialchars($eventData['zip'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Stadt:</label>
                <input type="text" name="city" value="<?= htmlspecialchars($eventData['city'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Land:</label>
                <input type="text" name="country" value="<?= htmlspecialchars($eventData['country'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <button type="submit"><?= $editing ? 'Änderungen speichern' : 'Event erstellen'; ?></button>
                <a href="events-verwalten.php" class="button-cancel">Abbrechen</a>
            </form>

            <script>
                // ... (Dein existierendes JavaScript für Location-Suche) ...
                // ... Es ist soweit sicher, da es die Daten vom Server holt und das DOM manipuliert.
                // ... Achte darauf, dass 'item.textContent' (sicher) statt 'item.innerHTML' (unsicher) genutzt wird.
                // ... Dein Original-Code nutzte textContent, das ist gut!
            </script>
        <?php endif; ?>
    </div>

    <div class="card top-margin">
        <h2>Deine Events</h2>
        <?php $events = getUserEvents(); // Holt alle Events des Users ?>
        <?php if (!empty($events) && is_array($events)): ?>
            <table class="entry-list">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Ort</th>
                        <th>Datum</th>
                        <th>Status</th>
                        <th>Aktionen</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($events as $event): ?>
                        <?php
                        // SICHERHEIT: Alles escapen für die Ausgabe
                        $docId  = htmlspecialchars($event['documentId'] ?? '', ENT_QUOTES, 'UTF-8');
                        $title  = htmlspecialchars($event['Title'] ?? 'Unbenannt', ENT_QUOTES, 'UTF-8');
                        $city   = htmlspecialchars($event['city'] ?? '', ENT_QUOTES, 'UTF-8');
                        $date   = htmlspecialchars($event['start_date'] ?? '', ENT_QUOTES, 'UTF-8');
                        $rawStatus = $event['status'] ?? '';
                        // Status-Check braucht kein Escaping für die Logik, aber für die Ausgabe (falls wir ihn direkt ausgäben)
                        $statusLabel = ($rawStatus === 'published') ? 'veröffentlicht' : 'Entwurf';
                        $isDraftParam = ($rawStatus === 'published') ? 'published' : 'draft';
                        ?>
                        <tr>
                            <td data-label="Name"><?= $title; ?></td>
                            <td data-label="stadt"><?= $city; ?></td>
                            <td data-label="Datum"><?= $date; ?></td>
                            <td data-label="Status"><?= $statusLabel; ?></td>
                            <td data-label="Aktionen">
                                <a href="?is_draft=<?= $isDraftParam ?>&edit=<?= $docId; ?>">Bearbeiten</a> |
                                <a href="?delete=<?= $docId; ?>" onclick="return confirm('Event wirklich löschen?');">Löschen</a>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        <?php else: ?>
            <p>Keine Events vorhanden.</p>
        <?php endif; ?>
    </div>
</div>
<?php include("../../inc/footer.php"); ?>