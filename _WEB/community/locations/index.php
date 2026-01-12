<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once(__DIR__ . '/../../inc/config.php');
require_once(__DIR__ . '/../../inc/functions.php');

// 1. SICHERHEIT: Login erzwingen
requireLogin();

include("../../inc/header_auth.php");

$userProfileId = $_SESSION['profile']['documentId'] ?? null;
$jwt = $_SESSION['jwt'] ?? null;
$apiBase = STRAPI_API_BASE . '/locations';

if (!$userProfileId || !$jwt) {
    die("Fehler: Sitzung ungültig. Bitte neu einloggen.");
}

$showForm     = false;
$editing      = isset($_GET['edit']) ? $_GET['edit'] : null;
$locationData = null;
$message      = '';

/**
 * =================================================================
 * DISCARD DRAFT - Änderungen verwerfen
 * =================================================================
 */
if (isset($_GET['discard'])) {
    $discardId = $_GET['discard'];

    // API Call zum Verwerfen des Drafts
    $ch = curl_init("$apiBase/" . urlencode($discardId) . "/discard-draft");
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => "POST",
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
        $message = "Änderungen wurden verworfen. Die veröffentlichte Version bleibt bestehen.";
    } elseif ($httpCode === 403) {
        $message = "Fehler: Du hast keine Berechtigung, diese Änderungen zu verwerfen.";
    } elseif ($httpCode === 400) {
        $message = "Fehler: Es gibt keine unbestätigten Änderungen zum Verwerfen.";
    } else {
        $message = "Fehler beim Verwerfen (API-Status: $httpCode)";
    }
}

/**
 * =================================================================
 * DELETE Location (Strapi Policy prüft Berechtigung)
 * =================================================================
 */
if (isset($_GET['delete'])) {
    $deleteId = $_GET['delete'];

    $ch = curl_init("$apiBase/" . urlencode($deleteId));
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
        $message = "Eintrag wurde gelöscht";
    } elseif ($httpCode === 403) {
        $message = "Fehler: Du hast keine Berechtigung, diesen Eintrag zu löschen.";
    } else {
        $message = "Fehler beim Löschen (API-Status: $httpCode)";
    }
}

/**
 * =================================================================
 * EDITING Location (Daten laden)
 * =================================================================
 */
if ($editing) {
    $loadedData = getUserLocations($editing, true);

    if ($loadedData) {
        $locationData = $loadedData;
        $showForm = true;
    } else {
        $message = "Fehler: Eintrag nicht gefunden oder keine Berechtigung.";
        $showForm = false;
    }
}

/**
 * =================================================================
 * CREATE / UPDATE Location
 * =================================================================
 */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {

    $isUpdate = !empty($_POST['documentId']);

    $postData = [
        'data' => [
            'Titel'             => $_POST['Titel'] ?? '',
            'Strasse'           => $_POST['Strasse'] ?? '',
            'Hausnummer'        => $_POST['Hausnummer'] ?? '',
            'PLZ'               => $_POST['PLZ'] ?? '',
            'Ort'               => $_POST['Ort'] ?? '',
            'Typ'               => $_POST['Typ'] ?? '',
            'allow_user_events' => isset($_POST['allow_user_events']) ? (bool)$_POST['allow_user_events'] : false,
            'Beschreibung'      => $_POST['Beschreibung'] ?? '',
            'Mail'              => filter_var($_POST['Mail'] ?? '', FILTER_VALIDATE_EMAIL) ?: null,
            'Website'           => filter_var($_POST['Website'] ?? '', FILTER_VALIDATE_URL) ?: null,
            'Telefon'           => !empty($_POST['Telefon']) ? $_POST['Telefon'] : null,
        ]
    ];

    $coords = getCoordinates(
        ($_POST['Strasse'] ?? '') . " " . ($_POST['Hausnummer'] ?? ''),
        $_POST['PLZ'] ?? '',
        $_POST['Ort'] ?? ''
    );
    if ($coords) {
        $postData['data']['coordinates'] = $coords;
    }

    if ($isUpdate) {
        $locId = $_POST['documentId'];
        $url = "$apiBase/" . urlencode($locId);
        $method = "PUT";
        $successMessage = "Eintrag wurde bearbeitet";
    } else {
        $url = $apiBase;
        $method = "POST";
        $successMessage = "Eintrag wurde erstellt";
    }

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $jwt",
        "Content-Type: application/json"
    ]);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_errno($ch);
    curl_close($ch);

    if ($curlError) {
        $message = "Netzwerkfehler beim Speichern";
    } elseif ($httpCode >= 200 && $httpCode < 300) {
        $responseData = json_decode($response, true);
        $publishedAt = $responseData['data']['publishedAt'] ?? null;
        
        if ($publishedAt) {
            $message = $successMessage . " und wurde veröffentlicht.";
        } else {
            $message = $successMessage . " als Entwurf.";
        }
        
        $showForm = false;
    } elseif ($httpCode === 403) {
        $message = "Fehler: Keine Berechtigung.";
    } else {
        $message = "Fehler beim Speichern (API-Status: $httpCode)";
    }
}
?>

<div class="content content-plain top-margin">
    <?php if ($message): ?>
        <div class="card success"><?= htmlspecialchars($message, ENT_QUOTES, 'UTF-8') ?></div>
    <?php endif; ?>

    <div class="card">
        <h1>Locations verwalten</h1>

        <?php if (!$showForm && !isset($_GET['new'])): ?>
            <form method="GET">
                <button type="submit" name="new" value="1">Neue Location eintragen</button>
            </form>
        <?php endif; ?>

        <?php if ($showForm || isset($_GET['new'])): ?>
            <form method="POST" class="top-margin">
                <h2><?= $editing ? 'Location bearbeiten' : 'Neue Location erstellen' ?></h2>

                <?php if (!empty($locationData['documentId'])): ?>
                    <input type="hidden" name="documentId" value="<?= htmlspecialchars($locationData['documentId'], ENT_QUOTES, 'UTF-8'); ?>">
                <?php endif; ?>

                <label>Name / Titel:</label>
                <input type="text" name="Titel" value="<?= htmlspecialchars($locationData['Titel'] ?? '', ENT_QUOTES, 'UTF-8'); ?>" required>

                <label>Straße:</label>
                <input type="text" name="Strasse" value="<?= htmlspecialchars($locationData['Strasse'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Hausnummer:</label>
                <input type="text" name="Hausnummer" value="<?= htmlspecialchars($locationData['Hausnummer'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>PLZ:</label>
                <input type="text" name="PLZ" value="<?= htmlspecialchars($locationData['PLZ'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Ort:</label>
                <input type="text" name="Ort" value="<?= htmlspecialchars($locationData['Ort'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Typ:</label>
                <select name="Typ" required>
                    <?php
                    $typen = ['Geschäft', 'Cafe', 'Club', 'Location'];
                    foreach ($typen as $typ) {
                        $selected = ($locationData['Typ'] ?? '') === $typ ? 'selected' : '';
                        echo "<option value=\"" . htmlspecialchars($typ, ENT_QUOTES, 'UTF-8') . "\" $selected>" . htmlspecialchars($typ, ENT_QUOTES, 'UTF-8') . "</option>";
                    }
                    ?>
                </select>

                <label>Beschreibung:</label>
                <textarea name="Beschreibung"><?= htmlspecialchars($locationData['Beschreibung'] ?? '', ENT_QUOTES, 'UTF-8'); ?></textarea>

                <label>Telefon:</label>
                <input type="text" name="Telefon" value="<?= htmlspecialchars($locationData['Telefon'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Mail:</label>
                <input type="email" name="Mail" value="<?= htmlspecialchars($locationData['Mail'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <label>Website:</label>
                <input type="url" name="Website" value="<?= htmlspecialchars($locationData['Website'] ?? '', ENT_QUOTES, 'UTF-8'); ?>">

                <div class="form-group">
                  <label>
                    <input type="checkbox" name="allow_user_events" value="1" <?= !empty($locationData['allow_user_events']) ? 'checked' : '' ?>>
                    Andere Benutzer dürfen Events für diese Location veröffentlichen
                  </label>
                </div>

                <button type="submit"><?= $editing ? 'Änderungen speichern' : 'Eintrag erstellen'; ?></button>
                <a href="locations-verwalten.php" class="button-cancel">Abbrechen</a>
            </form>
        <?php endif; ?>
    </div>

    <div class="card top-margin">
        <h2>Deine Locations</h2>
        <?php 
        $locations = getUserLocations(); 
        if (!empty($locations) && is_array($locations)): 
        ?>
            <table class="entry-list">
                <thead>
                    <tr>
                        <th></th>
                        <th>Name</th>
                        <th>Ort</th>
                        <th>Typ</th>
                        <th>Status</th>
                        <th>Aktionen</th>
                    </tr>
                </thead>
                <tbody>
                <?php foreach ($locations as $location): 
                    $locDocId = htmlspecialchars($location['documentId'] ?? '', ENT_QUOTES, 'UTF-8');
                    $locTitle = htmlspecialchars($location['Titel'] ?? 'Unbenannt', ENT_QUOTES, 'UTF-8');
                    $locCity  = htmlspecialchars($location['Ort'] ?? '', ENT_QUOTES, 'UTF-8');
                    $locType  = htmlspecialchars($location['Typ'] ?? '', ENT_QUOTES, 'UTF-8');
                    
                    // Status aus API Metadata
                    $hasPendingChanges = $location['_hasPendingChanges'] ?? false;
                    $status = $location['_status'] ?? 'unknown';
                    
                    if ($status === 'pending' || $hasPendingChanges) {
                        $locStatus = '<span style="color: orange;">⏳ Änderungen warten auf Freigabe</span>';
                    } elseif ($status === 'published') {
                        $locStatus = '<span style="color: green;">✓ Veröffentlicht</span>';
                    } elseif ($status === 'draft') {
                        $locStatus = '<span style="color: gray;">📝 Entwurf</span>';
                    } else {
                        $locStatus = '<span style="color: gray;">?</span>';
                    }

                    $locIcon = 'fa-question';
                    if ($location['Typ'] == 'Geschäft') $locIcon = 'fa-store';
                    if ($location['Typ'] == 'Cafe') $locIcon = 'fa-mug-saucer';
                    if ($location['Typ'] == 'Club') $locIcon = 'fa-people-group';
                    if ($location['Typ'] == 'Location') $locIcon = 'fa-location-dot';
                ?>
                    <tr>
                        <td data-label=""><i class="fa-solid <?= $locIcon ?>"></i></td>
                        <td data-label="Name"><?= $locTitle; ?></td>
                        <td data-label="City"><?= $locCity; ?></td>
                        <td data-label="Typ"><?= $locType; ?></td>
                        <td data-label="Status"><?= $locStatus; ?></td>
                        <td data-label="Aktionen">
                            <a href="?edit=<?= $locDocId; ?>">Bearbeiten</a> |
                            <?php if ($hasPendingChanges): ?>
                                <a href="?discard=<?= $locDocId; ?>" onclick="return confirm('Willst du deine Änderungen wirklich verwerfen? Die veröffentlichte Version bleibt bestehen.');">Änderungen verwerfen</a> |
                            <?php endif; ?>
                            <a href="?delete=<?= $locDocId; ?>" onclick="return confirm('Willst du diesen Eintrag wirklich löschen?');">Löschen</a>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        <?php else: ?>
            <p>Keine Locations vorhanden.</p>
        <?php endif; ?>
    </div>
</div>

<?php include("../../inc/footer.php"); ?>
