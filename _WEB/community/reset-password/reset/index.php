<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once __DIR__ . '/../../../inc/config.php';
require_once __DIR__ . '/../../../inc/functions.php'; // Nötig für Helfer

include("../../../inc/header.php");

$errorMessage = '';
$successMessage = '';
$showForm = false;
// SICHERHEIT: Token bereinigen (nur zur Vorsicht)
$token = isset($_GET['code']) ? trim($_GET['code']) : (isset($_GET['token']) ? trim($_GET['token']) : null);

// Funktion zur Überprüfung der Passwortsicherheit (beibehalten, ist gut)
function isPasswordSecure($password) {
    if (strlen($password) < 6) return 'Das Passwort muss mindestens 6 Zeichen lang sein.';
    if (!preg_match('/\d/', $password)) return 'Das Passwort muss mindestens eine Zahl enthalten.';
    if (!preg_match('/[A-Z]/', $password)) return 'Das Passwort muss mindestens einen Großbuchstaben enthalten.';
    // Sonderzeichen-Check lockern? Viele User nutzen nur ! oder ?
    // if (!preg_match('/[^\w]/', $password)) return 'Das Passwort muss mindestens ein Sonderzeichen enthalten.';
    return true;
}

// Logik-Korrektur: Formular anzeigen, wenn Token existiert.
// Wir validieren den Token erst beim Absenden, um "Double-Spend" zu vermeiden.
if ($token) {
    $showForm = true;

    if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_POST['new_password'])) {
        $newPassword = $_POST['new_password'];
        $passwordCheck = isPasswordSecure($newPassword);

        if ($passwordCheck !== true) {
            $errorMessage = $passwordCheck;
        } else {
            // API-Aufruf zum Setzen des Passworts
            // HINWEIS: /auth/reset-password ist oft öffentlich.
            // Wir nutzen den Admin-Helfer für Robustheit (cURL), aber ein Public-Helfer ginge auch.
            $url = STRAPI_API_BASE . '/auth/reset-password';
            $payload = [
                'code' => $token,
                'password' => $newPassword,
                'passwordConfirmation' => $newPassword
            ];

            $result = makeAdminStrapiApiCall('POST', $url, $payload);

            if ($result['success']) {
                $successMessage = 'Dein Passwort wurde erfolgreich geändert.';
                $showForm = false;
            } else {
                // Fehler: Token ungültig/abgelaufen oder anderer Fehler
                // SICHERHEIT: Generische Fehlermeldung für User, Details nur loggen
                debugLog("Reset Password Error for token $token: " . json_encode($result));
                $errorMessage = 'Der Link ist ungültig oder abgelaufen. Bitte fordere einen neuen an.';
            }
        }
    }
} else {
    $errorMessage = 'Ungültiger Aufruf. Kein Token vorhanden.';
}
?>

<div class="content top-margin content-plain">
    <h1>Neues Passwort setzen</h1>

    <?php if ($errorMessage): ?>
        <div class="card error"><?= esc($errorMessage) ?></div>
    <?php endif; ?>

    <?php if ($successMessage): ?>
        <div class="card success"><?= esc($successMessage) ?></div>
    <?php endif; ?>

    <?php if ($showForm): ?>
        <div class="card">
            <form method="post">
                <p>
                    <label for="new_password">Neues Passwort:</label><br>
                    <input type="password" name="new_password" id="new_password" required>
                </p>
                <p>
                    <input type="submit" value="Passwort setzen" class="button button-primary">
                </p>
            </form>
        </div>
    <?php endif; ?>

    <div class="card">
        <a href="/community/login/">Zurück zur Anmeldung</a>
    </div>
</div>

<?php include("../../../inc/footer.php"); ?>