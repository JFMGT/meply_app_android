<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once __DIR__ . '/../../inc/config.php';
require_once __DIR__ . '/../../inc/functions.php'; // Nötig für Helfer

include("../../inc/header.php");

$errorMessage = '';
$successMessage = '';

// HINWEIS: Wir nutzen hier den Admin-Helfer für Robustheit.
// Normalerweise ist dieser Endpunkt in Strapi öffentlich und bräuchte gar keinen Token.
// Wenn er öffentlich ist, könnte man auch einen einfachen 'makePublicApiCall'-Helfer nutzen.

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = trim($_POST['user_email'] ?? '');

    if (empty($email)) {
        $errorMessage = 'Bitte gib deine E-Mail-Adresse ein.';
    } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
         $errorMessage = 'Bitte gib eine gültige E-Mail-Adresse ein.';
    } else {
        // API-Aufruf vorbereiten
        // KORREKTUR: Pfad angepasst (kein doppeltes /api/)
        $url = STRAPI_API_BASE . '/auth/forgot-password';
        $payload = ['email' => $email];

        // Wir nutzen den Admin-Helfer, da er robust ist (cURL, Fehlerhandling).
        // Falls der Endpunkt öffentlich ist, schadet der Token meist nicht.
        $result = makeAdminStrapiApiCall('POST', $url, $payload);

        if ($result['success']) {
            // ERFOLG: Generische Meldung anzeigen (WICHTIG für Sicherheit!)
            $successMessage = 'Wenn die E-Mail existiert, haben wir dir einen Link zum Zurücksetzen geschickt.';
        } else {
            // FEHLER: Loggen, aber dem User nur generische Meldung zeigen
            // Strapi gibt bei 'forgot-password' oft auch 200 OK zurück, selbst wenn die Mail nicht existiert (Sicherheitsfeature).
            // Wenn wir hier landen, war es ein echter technischer Fehler (z.B. Mailserver down).
            debugLog("Forgot Password Error for $email: " . json_encode($result));
            $errorMessage = 'Es gab ein technisches Problem bei der Anfrage. Bitte versuche es später erneut.';
        }
    }
}
?>

<div class="content top-margin content-plain">
    <h1>Passwort zurücksetzen</h1>

    <div class="card">
        <p>Gib deine E-Mail-Adresse ein, mit der du dich registriert hast. Wir senden dir dann einen Link, um dein Passwort zurückzusetzen.</p>
    </div>

    <?php if ($errorMessage): ?>
        <div class="card error">
            <?= esc($errorMessage) ?>
        </div>
    <?php endif; ?>

    <?php if ($successMessage): ?>
        <div class="card success">
            <?= esc($successMessage) ?>
        </div>
    <?php endif; ?>

    <div class="card">
        <form name="resetpasswordform" id="resetpasswordform" action="" method="post">
            <p class="login-username">
                <label for="user_email">E-Mail-Adresse</label>
                <input type="email" name="user_email" id="user_email" autocomplete="email" class="input" value="<?= esc($_POST['user_email'] ?? '') ?>" size="20" required />
            </p>

            <p class="login-submit">
                <input type="submit" name="reset-submit" id="reset-submit" class="button button-primary" value="Link anfordern" />
            </p>
        </form>
    </div>

    <div class="card">
        <a href="/community/login.php">Zurück zur Anmeldung</a>
    </div>
</div>

<?php include("../inc/footer.php"); ?>