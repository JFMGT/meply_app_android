<?php
include("../inc/header_auth.php");
require_once __DIR__ . '/../inc/functions.php';
?>

<div class="card content content-plain top-margin">
<?php 

// Konfiguration
$strapiUrl = STRAPI_API_BASE;
$strapiUrlImageUpload = STRAPI_IMAGE_BASE;

$jwt = $_SESSION['jwt'] ?? null;
if (!$jwt) {
  echo json_encode(['error' => 'Nicht eingeloggt']);
  exit;
}

// Funktion: Bild zu Strapi hochladen und Media-ID zurückgeben
function uploadAvatarToStrapi($file, $jwt, $strapiUrl, $userId = false) {
    // ➤ Validierungsparameter
    $allowedTypes = ['image/jpeg', 'image/png'];
    $maxSize = 1 * 1024 * 1024; // 1MB
    $maxWidth = 500;
    $maxHeight = 500;

    // ➤ MIME-Type prüfen
    if (!in_array($file['type'], $allowedTypes)) {
        echo "<div class='error'><p>Nur JPG oder PNG Bilder sind erlaubt.</p></div>";
        return null;
    }

    // ➤ Dateigröße prüfen
    if ($file['size'] > $maxSize) {
        echo "<div class='error'><p>Das Bild ist zu groß. Maximal 1MB erlaubt.</p></div>";
        return null;
    }

    // ➤ Bilddimensionen prüfen
    $imageSize = getimagesize($file['tmp_name']);
    if (!$imageSize) {
        echo "<div class='error'><p>Die Bilddatei ist ungültig oder beschädigt.</p></div>";
        return null;
    }

    if ($imageSize[0] > $maxWidth || $imageSize[1] > $maxHeight) {
        echo "<div class='error'><p>Das Bild darf maximal {$maxWidth}×{$maxHeight} Pixel groß sein.</p></div>";
        return null;
    }

    // ➤ Bild ist gültig – hochladen
    $url = "$strapiUrl/upload";
    $originalName = pathinfo($file['name'], PATHINFO_FILENAME);
    $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
    $customName = "avatar_{$userId}_{$originalName}.{$extension}";

    $curlFile = new CURLFile($file['tmp_name'], $file['type'], $customName);
    $postFields = ['files' => $curlFile];

    $headers = [
        "Authorization: Bearer $jwt"
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $postFields);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

    $response = curl_exec($ch);
    curl_close($ch);

    $data = json_decode($response, true);
    return $data[0]['id'] ?? null;
}


$userId = $_SESSION['user']['documentId']; // Benutzerprofil-ID in Strapi
$profileId = getProfileData(true);
$profileDocumentId = $_SESSION['profile']['documentId'];
// Funktion: Benutzerdaten aus Strapi abrufen
function getProfile($strapiUrl, $profileId, $jwt) {
    $url = "$strapiUrl/profiles/$profileId";
    $headers = [
        "Authorization: Bearer $jwt",
        "Content-Type: application/json"
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $response = curl_exec($ch);
    curl_close($ch);
    $data = json_decode($response, true);
    return $data['data']['attributes'] ?? [];
}



// Funktion: Profil aktualisieren
function updateProfile($strapiUrl, $jwt, $payload) {
    $payload = array_filter($payload, fn($v) => $v !== '');
    $url = "$strapiUrl/profiles/me";
    $headers = [
        "Authorization: Bearer $jwt",
        "Content-Type: application/json"
    ];

    $body = json_encode(['data' => $payload]);

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PUT");
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

    $response = curl_exec($ch);
    curl_close($ch);

    return json_decode($response, true);
}

// Formularverarbeitung
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $formId = $_POST['form_id'] ?? '';

    if ($formId === 'profile_form') {
        $payload = [
            'username' => $_POST['username'] ?? '',
            'birthDate' => ($_POST['birthDate'] ?? '') !== '' ? $_POST['birthDate'] : null,
            'postalCode' => $_POST['postalCode'] ?? '',
            'city' => $_POST['city'] ?? '',
            'searchRadius' => isset($_POST['searchRadius']) ? (int)$_POST['searchRadius'] : null,
            'followPrivacy' => isset($_POST['followPrivacy']) ? 'open' : 'request',
            'showInUserList' => isset($_POST['showInUserList']) ? true : false,
            'allowProfileView' => isset($_POST['allowProfileView']) ? true : false,
            'showBoardGameRatings' => isset($_POST['showBoardGameRatings']) ? true : false,
            'gender' => $_POST['gender'] ?? null,
            'boardgamegeekProfile' => $_POST['boardgamegeekProfile'] ?? '',
            'boardGameArenaUsername' => $_POST['boardGameArenaUsername'] ?? ''
        ];

        // ➕ Koordinaten hinzufügen (wenn PLZ & Ort gesetzt sind)
        if (!empty($payload['postalCode']) && !empty($payload['city'])) {
            $coords = getCoordinates('', $payload['postalCode'], $payload['city']);
            if ($coords) {
                // Als JSON speichern
                $payload['cords'] = json_encode($coords);
                
                // Zusätzlich lat/lng explizit speichern
                $payload['latitude'] = $coords['lat'];
                $payload['longitude'] = $coords['lng'];
            }
        }

         }

         if ($formId === 'profile_img') {
    $payload = [];

    // ➕ Neues Avatar hochladen
    if (!empty($_FILES['avatar']['tmp_name']) && $_POST['avatarDelete'] != "1") {
            // 🔁 Vorheriges Avatar löschen (wenn vorhanden)
            $oldMediaId = $_SESSION['profile']['avatar'][0]['id'] ?? null;
            if ($oldMediaId) {
                deleteMediaFromStrapi($oldMediaId);

                if (!$deleteResult['success']) {
                    debugLog("Konnte altes Avatar (ID: $oldMediaId) nicht löschen.");
                }
            }

            $avatarId = uploadAvatarToStrapi($_FILES['avatar'], $jwt, $strapiUrl, $userId);
            if ($avatarId) {
                $payload['avatar'] = $avatarId;
            }
        }

        // ➖ Avatar löschen (durch Button-Klick)
        if (!empty($_POST['avatarDelete']) && $_POST['avatarDelete'] == "1") {
            $payload['avatar'] = null;

            $mediaId = $_SESSION['profile']['avatar'][0]['id'] ?? null;
            if ($mediaId) {
                deleteMediaFromStrapi($mediaId);

                if (!$deleteResult['success']) {
                    debugLog("Konnte Avatar (ID: $mediaId) beim Löschen nicht entfernen.");
                }
            }
        }
    }

       
    $updateResult = updateProfile($strapiUrl, $jwt, $payload);
    echo "<div class='success'><p><strong>Profil aktualisiert.</strong></p></div>";
   
}


// Aktuelle Profildaten holen
$profile = getProfile($strapiUrl, $userId, $jwt);
?>
<style>
    .form-section {
        display: flex;
        flex-wrap: wrap;
        gap: 3rem;
        margin-bottom: 2rem;
    }

    .form-group {
        flex: 1 1 45%;
        display: flex;
        flex-direction: column;
    }

    .form-group.full {
        flex: 1 1 100%;
    }

    label {
        
        margin-bottom: 0.25rem;
    }

    input[type="text"],
    input[type="date"],
    input[type="number"],
    select {
        padding: 0.5rem;
        border-radius: 6px;
        border: 1px solid #ccc;
        font-size: 1rem;
        width: 100%;
        width:calc(100% - 48px);
        margin-right:10px;
    }

    input[type="checkbox"] {
        margin-right: 0.5rem;
    }

    .checkbox-group {
        margin-bottom: 1rem;
        display: grid;
  grid-template-columns: 50px 1fr; /* 1. Spalte: 100px, 2. Spalte: restlicher Platz */
  align-items: start;
  gap: 0.5rem;
    }

    .checkbox-group label{
        padding-top:5px;
    }

    button {
        padding: 0.75rem 1.5rem;
        font-size: 1rem;
        background-color: #4caf50;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
    }

    button:hover {
        background-color: #45a049;
    }

    img.avatar-preview {
        margin-top: 0.5rem;
        border-radius: 6px;
    }

    #avatarDeleteBtn.delete-pending {
    background-color: #e53935;
    color: white;
    border: none;
}

#avatarDeleteBtn.delete-pending::after {
    content: " (Wird beim Speichern gelöscht)";
    font-weight: bold;
    font-size: 0.9em;
}
</style>

<h1>Profil</h1>
<span>
Hier kannst du deine Benutzerinformationen aktualisieren. Alle Angaben sind freiwillig, aber sie können dabei helfen, die passenden Mitspieler für dich zu finden. Ob Alter, Geschlecht oder deine Lieblingsspiele – diese Informationen ermöglichen es anderen, dich besser kennenzulernen und gemeinsame Interessen zu entdecken.
</span><br><br>
<?php 
   $userId  = getProfileData(true);
?>
<!-- HTML-Formular -->
<form method="POST">
     <input type="hidden" name="form_id" value="profile_form">
    <div class="form-section">
        <div class="form-group">
            <label>Benutzername:</label>
            <input type="text" name="username" value="<?= htmlspecialchars($_SESSION['profile']['username'] ?? '') ?>" required>
        </div>

        <div class="form-group">
            <label>Geburtsdatum:</label>
            <input type="date" name="birthDate" value="<?= htmlspecialchars($_SESSION['profile']['birthDate'] ?? '') ?>">
        </div>
    </div>

   

    <div class="form-section">
        <div class="form-group">
            <label>Postleitzahl:</label>
            <input type="text" name="postalCode" value="<?= htmlspecialchars($_SESSION['profile']['postalCode'] ?? '') ?>">
        </div>

        <div class="form-group">
            <label>Stadt:</label>
            <input type="text" name="city" value="<?= htmlspecialchars($_SESSION['profile']['city'] ?? '') ?>">
        </div>
    </div>

    <div class="form-section">
        <div class="form-group">
            <label>Suchradius (km):</label>
            <input type="number" name="searchRadius" value="<?= htmlspecialchars($_SESSION['profile']['searchRadius'] ?? '') ?>" min="0">
        </div>

        <div class="form-group">
            <label>Geschlecht:</label>
            <select name="gender" style="height:48px">
                <option value="none" <?= ($_SESSION['profile']['gender'] ?? '') === 'none' ? 'selected' : '' ?>>Keine Angabe</option>
                <option value="female" <?= ($_SESSION['profile']['gender'] ?? '') === 'female' ? 'selected' : '' ?>>Weiblich</option>
                <option value="male" <?= ($_SESSION['profile']['gender'] ?? '') === 'male' ? 'selected' : '' ?>>Männlich</option>
                <option value="diverse" <?= ($_SESSION['profile']['gender'] ?? '') === 'diverse' ? 'selected' : '' ?>>Divers</option>
                <option value="other" <?= ($_SESSION['profile']['gender'] ?? '') === 'other' ? 'selected' : '' ?>>anderes</option>
            </select>
        </div>
    </div>

    <div class="form-section">
        <div class="form-group">
            <label>BoardGameGeek Profil:</label>
            <input type="text" name="boardgamegeekProfile" value="<?= htmlspecialchars($_SESSION['profile']['boardgamegeekProfile'] ?? '') ?>">
        </div>

        <div class="form-group">
            <label>BoardGameArena Benutzername:</label>
            <input type="text" name="boardGameArenaUsername" value="<?= htmlspecialchars($_SESSION['profile']['boardGameArenaUsername'] ?? '') ?>">
        </div>
    </div>

    <div class="form-section">
        <div class="form-group full checkbox-group">
            <input type="checkbox" name="showInUserList" value="1" <?= !empty($_SESSION['profile']['showInUserList']) ? 'checked' : '' ?>>
            <label for="showInUserList">Soll dein Benutzer auf der Benutzerkarte dargestellt werden? Gesetzt wird dein Marker an einem zentralen Punkt innerhalb deines Postleitzahlengebiets.</label>
        </div>
        <?php 
        $usersCanFollow = true;
        if($_SESSION['profile']['usersCanFollow'] == 'request'){
            $usersCanFollow = false;
        }

        ?>
        <div class="form-group full checkbox-group">
            <input type="checkbox" name="followPrivacy" value="1" <?= !empty($usersCanFollow) ? 'checked' : '' ?>>
            <label for="allowProfileView">Andere dürfen mir ohne Bestätigung folgen. Alternativ kannst du jeden Follower einzeln bestätigen.</label>
        </div>

        <div class="form-group full checkbox-group">
            <input type="checkbox" name="showBoardGameRatings" value="1" <?= !empty($_SESSION['profile']['showBoardGameRatings']) ? 'checked' : '' ?>>
            <label for="showBoardGameRatings">Meine Brettspielbewertungen (inkl. Anzahl der Sterne) dürfen anderen Mitgliedern angezeigt werden.<br>Wenn du Spielbewertungen anderer Nutzer sehen oder Spielvorschläge erhalten möchtest, musst du zustimmen, dass deine eigenen Bewertungen ebenfalls sichtbar sind. Diese Einstellung kannst du jederzeit ändern.</label>
        </div>

        <div class="form-group full checkbox-group">
            <input type="checkbox" name="allowProfileView" value="1" <?= !empty($_SESSION['profile']['allowProfileView']) ? 'checked' : '' ?>>
            <label for="allowProfileView">Mein Profil darf von anderen gesehen werden</label>
        </div>
    </div>

  

    <div class="form-section">
        <button type="submit">Speichern</button>
    </div>
</form>

</div>
<div class="card content content-plain">
<h2>Profilbild bearbeiten</h2>
<form method="POST" enctype="multipart/form-data">
    <input type="hidden" name="form_id" value="profile_img">
    <!-- Felder für Einstellungen -->
    <div class="form-section">
        <div class="form-group full">
            <label>Avatar:</label>
            <p>Du kannst hier ein eigenes Profilbild hochladen. Erlaubt sind JPG- oder PNG-Dateien mit einer maximalen Größe von 1 MB und einer Auflösung von höchstens 500×500 Pixeln.</p>
            <?php if (!empty($_SESSION['profile']['avatar'][0]['url'])): ?>
    <img src="<?= $strapiUrlImageUpload . $_SESSION['profile']['avatar'][0]['url'] ?>" alt="Avatar" width="100" class="avatar-preview">
<?php endif; ?>

            <input type="file" name="avatar" accept="image/*">
            <?php if (!empty($_SESSION['profile']['avatar']['url'])): ?>
                <img src="<?= $strapiUrl . $_SESSION['profile']['avatar']['url'] ?>" alt="Avatar" width="100" class="avatar-preview">
            <?php endif; ?>
             <button type="button" id="avatarDeleteBtn" onclick="toggleAvatarDelete()">🗑️ Avatar löschen</button>
            <input type="hidden" name="avatarDelete" id="avatarDelete" value="0">
            <br><div class="form-section">
        <button type="submit">Speichern</button>
    </div>
        </div>
    </div>
</form>
</div>


<div class="card content content-plain">
<?php

// Invite-Codes holen/erstellen
$result = getMyInviteCodes($jwt);

if (!$result) {
    echo "Fehler beim Laden der Codes";
    
}else{

// Daten sind verfügbar

$codes = $result['codes'];
$stats = $result['stats'];

echo "<h2>Deine Einladungscodes</h2>";
echo "<p>Teile einen Code mit einer Person, die du einladen möchtest. Ein Code kann nur einmal verwendet werden. Klicke auf das Kopieren-Symbol, um ihn in die Zwischenablage zu übernehmen.</p>";
echo "<p>Du hast {$stats['totalCodes']} Codes, davon {$stats['unusedCodes']} unbenutzt.</p>";

if (!empty($stats['newCodesGenerated']) && $stats['newCodesGenerated'] > 0) {
    echo "<p class='success'>{$stats['newCodesGenerated']} neue Codes wurden für dich erstellt.</p>";
}

if (!empty($stats['canGenerateMore']) && $stats['canGenerateMore'] > 0) {
    echo "<p>Du kannst noch {$stats['canGenerateMore']} weitere Codes bekommen.</p>";
}



echo "<h3>Deine Codes:</h3>";
echo "<ul class='invite-codes'>";

foreach ($codes as $code) {
    $isUsed = !empty($code['used']);
    $codeValue = htmlspecialchars((string)$code['code'], ENT_QUOTES, 'UTF-8');

    // 1) Status-Icon vorne
    $statusIcon = $isUsed
        ? "<i class='fa-solid fa-circle-check status-icon' aria-hidden='true'></i>"
        : "<i class='fa-regular fa-circle status-icon' aria-hidden='true'></i>";

    // 1) Wenn genutzt: Code durchstreichen
    $codeClass = $isUsed ? "code-text code-used" : "code-text";

    // Used-Date optional
    $usedDate = '';
    if (!empty($code['usedAt'])) {
        $usedDate = ' am ' . date('d.m.Y', strtotime($code['usedAt']));
    }

    // 2) Copy-Icon hinten nur bei ungenutzten Codes
    $copyHtml = '';
    if (!$isUsed) {
        $copyHtml = "
          <span class='copy-btn js-copy' role='button' tabindex='0'
                data-code='{$codeValue}'
                title='Code kopieren' aria-label='Code kopieren'>
            <i class='fa-regular fa-clipboard' aria-hidden='true'></i>
          </span>
          <span class='copy-feedback js-copy-feedback' aria-live='polite'>Kopiert</span>
        ";
    }

    echo "<li>";
    echo $statusIcon;
    echo "<span class='{$codeClass}'>{$codeValue}</span>";
    echo $isUsed
        ? "<span class='code-meta'>Verwendet{$usedDate}</span>"
        : "<span class='code-meta'>Verfügbar</span>";
    echo $copyHtml;
    echo "</li>";
}

echo "</ul>";

echo "<p>Limits: Max. {$stats['limits']['maxUnused']} unbenutzte, max. {$stats['limits']['maxTotal']} gesamt</p>";

echo <<<HTML
<script>
(function() {
  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text);
    }
    // Fallback für nicht-secure contexts / ältere Browser
    return new Promise(function(resolve, reject) {
      try {
        var ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.left = '-9999px';
        ta.style.top = '0';
        document.body.appendChild(ta);
        ta.focus();
        ta.select();
        var ok = document.execCommand('copy');
        document.body.removeChild(ta);
        ok ? resolve() : reject();
      } catch (e) {
        reject(e);
      }
    });
  }

  function showFeedback(btn) {
    var feedback = btn.parentElement.querySelector('.js-copy-feedback');
    if (!feedback) return;
    feedback.classList.add('show');
    window.setTimeout(function() {
      feedback.classList.remove('show');
    }, 1200);
  }

  function handleCopy(btn) {
    var code = btn.getAttribute('data-code');
    if (!code) return;

    copyText(code).then(function() {
      showFeedback(btn);
    }).catch(function() {
      // Optional: Fehler-Feedback
    });
  }

  document.querySelectorAll('.js-copy').forEach(function(btn) {
    btn.addEventListener('click', function() { handleCopy(btn); });
    btn.addEventListener('keydown', function(e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleCopy(btn);
      }
    });
  });
})();
</script>
HTML;

}
?>


</div>


<div class="card content content-plain">

<?php 
$deleteDate = $_SESSION['profile']['deleteDate'];
$buttonText = "Benutzer löschen";
$formTarget = "/community/privacy-consent/schedule-delete.php";
if (empty($deleteDate) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $deleteDate)) { ?>
  <h2>Benutzerkonto löschen</h2>
  <p>
    Wenn du dein Benutzerkonto löschen möchtest, starte den Vorgang mit einem Klick auf <strong>„Benutzer löschen“</strong>.<br>
    Dein Konto wird daraufhin zur Löschung vorgemerkt und nach 14 Tagen endgültig gelöscht.<br>
    In dieser Zeit kannst du die Löschung rückgängig machen, indem du dich erneut mit deinen Zugangsdaten anmeldest.<br>
    <strong>Bitte beachte:</strong> Du erhältst vor der Löschung keine Erinnerung per E-Mail.
  </p>
 <?php }else{ 

$buttonText = "Löschen abbrechen";
$formTarget = "/community/privacy-consent/cancel-delete.php";
    ?>
    <h2>Benutzerkonto löschen abbrechen</h2>
  <p>
    Dein Konto ist für die Löschung am <?= $deleteDate ?> vorgesehen. <br>
    Gibt erneut dein Passwort ein, um den Löschvorgang zu verhindern.
  </p>

    <?php } ?>
  <form id="delete-user" method="POST" action="<?= $formTarget; ?>">

    <button type="submit"><?= $buttonText ?></button>
  </form>
</div>
<script>
function toggleAvatarDelete() {
    const btn = document.getElementById('avatarDeleteBtn');
    const input = document.getElementById('avatarDelete');

    const isPendingDelete = input.value === "1";

    if (isPendingDelete) {
        // Abbrechen
        input.value = "0";
        btn.classList.remove('delete-pending');
        btn.textContent = "🗑️ Avatar löschen";
    } else {
        // Löschen vormerken
        input.value = "1";
        btn.classList.add('delete-pending');
        btn.textContent = "🗑️ Avatar löschen";
    }
}
</script>
<?php include("../inc/footer.php"); ?>
