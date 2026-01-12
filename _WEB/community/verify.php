<?php
/**
 * Email-Verifizierung - Meply.de Design
 * 
 * User sieht: https://meply.de/community/verify?token=xyz
 */

require_once '../inc/functions.php';

$token = $_GET['token'] ?? null;

if (!$token) {
    showErrorPage("Kein Bestätigungstoken übergeben.");
}

if (!preg_match('/^[a-f0-9]{64}$/i', $token)) {
    showErrorPage("Ungültiger Bestätigungslink.");
}

// Strapi API aufrufen
$url = STRAPI_API_BASE . "/auth/verify-email?token=" . urlencode($token);

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if (curl_errno($ch) || $httpCode !== 200) {
    debugLog("Verification failed: HTTP $httpCode | Response: $response");
    showErrorPage("Ein Fehler ist aufgetreten. Bitte versuche es später erneut.");
}

$data = json_decode($response, true);

if (!$data || !isset($data['success'])) {
    showErrorPage("Ungültige API-Response.");
}

if (!$data['success']) {
    if (isset($data['alreadyConfirmed']) && $data['alreadyConfirmed']) {
        showAlreadyConfirmedPage();
    } else {
        $errorMessage = $data['message'] ?? 'Verifizierung fehlgeschlagen.';
        showErrorPage($errorMessage);
    }
}

showSuccessPage($data['user'] ?? []);

// ========================================
// HELPER FUNCTIONS - Meply.de Design
// ========================================

function showSuccessPage($user) {
    ?>
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <title>Account bestätigt - Meply</title>
        <meta http-equiv="refresh" content="5;URL=/community/login.php">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background: #2c3e50;
                color: #ecf0f1;
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
            }
            
            .container {
                background: #34495e;
                border-radius: 8px;
                padding: 50px 40px;
                max-width: 500px;
                width: 100%;
                box-shadow: 0 10px 40px rgba(0,0,0,0.3);
                text-align: center;
            }
            
            .logo {
                font-size: 32px;
                font-weight: bold;
                color: #FFC107;
                margin-bottom: 10px;
            }
            
            .logo-subtitle {
                font-size: 14px;
                color: #95a5a6;
                margin-bottom: 40px;
            }
            
            .success-icon {
                font-size: 80px;
                margin-bottom: 30px;
                animation: scaleIn 0.6s ease-out;
            }
            
            @keyframes scaleIn {
                from { 
                    transform: scale(0) rotate(-180deg);
                    opacity: 0;
                }
                to { 
                    transform: scale(1) rotate(0);
                    opacity: 1;
                }
            }
            
            h1 {
                color: #FFC107;
                font-size: 28px;
                margin-bottom: 20px;
                font-weight: 600;
            }
            
            .welcome-text {
                color: #ecf0f1;
                font-size: 18px;
                margin-bottom: 15px;
            }
            
            .info-text {
                color: #95a5a6;
                font-size: 16px;
                line-height: 1.6;
                margin-bottom: 30px;
            }
            
            .countdown-container {
                margin: 30px 0;
            }
            
            .countdown-text {
                color: #95a5a6;
                font-size: 14px;
                margin-bottom: 10px;
            }
            
            .countdown {
                font-size: 60px;
                font-weight: bold;
                color: #FFC107;
                font-family: 'Arial', sans-serif;
            }
            
            .button {
                display: inline-block;
                background: #FFC107;
                color: #2c3e50;
                padding: 15px 40px;
                border-radius: 4px;
                text-decoration: none;
                font-weight: 600;
                font-size: 16px;
                transition: all 0.3s;
                border: none;
                cursor: pointer;
            }
            
            .button:hover {
                background: #FFD54F;
                transform: translateY(-2px);
                box-shadow: 0 5px 20px rgba(255, 193, 7, 0.4);
            }
            
            @media (max-width: 600px) {
                .container {
                    padding: 40px 30px;
                }
                
                h1 {
                    font-size: 24px;
                }
                
                .countdown {
                    font-size: 48px;
                }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="logo">MEPLY</div>
            <div class="logo-subtitle">Meet & Play</div>
            
            <div class="success-icon">✓</div>
            
            <h1>Account erfolgreich bestätigt!</h1>
            
            <p class="welcome-text">
                Willkommen bei Meply, <?php echo htmlspecialchars($user['username'] ?? 'Spieler', ENT_QUOTES); ?>! 🎲
            </p>
            
            <p class="info-text">
                Du kannst dich jetzt einloggen und mit anderen Brettspiel-Fans in deiner Nähe spielen.
            </p>
            
            <div class="countdown-container">
                <div class="countdown-text">Weiterleitung in</div>
                <div class="countdown" id="countdown">5</div>
                <div class="countdown-text">Sekunden</div>
            </div>
            
            <a href="/community/login.php" class="button">Jetzt anmelden</a>
        </div>
        
        <script>
            let seconds = 5;
            const countdownEl = document.getElementById('countdown');
            
            const interval = setInterval(() => {
                seconds--;
                countdownEl.textContent = seconds;
                
                if (seconds <= 0) {
                    clearInterval(interval);
                    window.location.href = '/community/login.php';
                }
            }, 1000);
        </script>
    </body>
    </html>
    <?php
    exit;
}

function showAlreadyConfirmedPage() {
    ?>
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <title>Bereits bestätigt - Meply</title>
        <meta http-equiv="refresh" content="3;URL=/community/login.php">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background: #2c3e50;
                color: #ecf0f1;
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
            }
            
            .container {
                background: #34495e;
                border-radius: 8px;
                padding: 50px 40px;
                max-width: 500px;
                width: 100%;
                box-shadow: 0 10px 40px rgba(0,0,0,0.3);
                text-align: center;
            }
            
            .logo {
                font-size: 32px;
                font-weight: bold;
                color: #FFC107;
                margin-bottom: 10px;
            }
            
            .logo-subtitle {
                font-size: 14px;
                color: #95a5a6;
                margin-bottom: 40px;
            }
            
            .info-icon {
                font-size: 80px;
                margin-bottom: 30px;
                color: #3498db;
            }
            
            h1 {
                color: #FFC107;
                font-size: 28px;
                margin-bottom: 20px;
                font-weight: 600;
            }
            
            p {
                color: #95a5a6;
                font-size: 16px;
                line-height: 1.6;
                margin-bottom: 15px;
            }
            
            .button {
                display: inline-block;
                background: #FFC107;
                color: #2c3e50;
                padding: 15px 40px;
                border-radius: 4px;
                text-decoration: none;
                font-weight: 600;
                font-size: 16px;
                transition: all 0.3s;
                margin-top: 20px;
            }
            
            .button:hover {
                background: #FFD54F;
                transform: translateY(-2px);
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="logo">MEPLY</div>
            <div class="logo-subtitle">Meet & Play</div>
            
            <div class="info-icon">ℹ</div>
            
            <h1>Account bereits bestätigt</h1>
            
            <p>Dein Account wurde bereits bestätigt.</p>
            <p>Du kannst dich direkt anmelden.</p>
            
            <a href="/community/login.php" class="button">Jetzt anmelden</a>
        </div>
    </body>
    </html>
    <?php
    exit;
}

function showErrorPage($message) {
    ?>
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <title>Fehler - Meply</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background: #2c3e50;
                color: #ecf0f1;
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
            }
            
            .container {
                background: #34495e;
                border-radius: 8px;
                padding: 50px 40px;
                max-width: 500px;
                width: 100%;
                box-shadow: 0 10px 40px rgba(0,0,0,0.3);
                text-align: center;
            }
            
            .logo {
                font-size: 32px;
                font-weight: bold;
                color: #FFC107;
                margin-bottom: 10px;
            }
            
            .logo-subtitle {
                font-size: 14px;
                color: #95a5a6;
                margin-bottom: 40px;
            }
            
            .error-icon {
                font-size: 80px;
                margin-bottom: 30px;
                color: #e74c3c;
            }
            
            h1 {
                color: #FFC107;
                font-size: 28px;
                margin-bottom: 20px;
                font-weight: 600;
            }
            
            .error-message {
                color: #ecf0f1;
                font-size: 16px;
                margin-bottom: 15px;
                padding: 15px;
                background: rgba(231, 76, 60, 0.1);
                border-left: 4px solid #e74c3c;
                border-radius: 4px;
            }
            
            p {
                color: #95a5a6;
                font-size: 16px;
                line-height: 1.6;
                margin-bottom: 15px;
            }
            
            .button {
                display: inline-block;
                background: #FFC107;
                color: #2c3e50;
                padding: 15px 40px;
                border-radius: 4px;
                text-decoration: none;
                font-weight: 600;
                font-size: 16px;
                transition: all 0.3s;
                margin-top: 20px;
            }
            
            .button:hover {
                background: #FFD54F;
                transform: translateY(-2px);
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="logo">MEPLY</div>
            <div class="logo-subtitle">Meet & Play</div>
            
            <div class="error-icon">✕</div>
            
            <h1>Fehler bei der Bestätigung</h1>
            
            <div class="error-message">
                <?php echo htmlspecialchars($message, ENT_QUOTES); ?>
            </div>
            
            <p>Bitte überprüfe den Link in deiner E-Mail oder kontaktiere unseren Support.</p>
            
            <a href="/" class="button">Zur Startseite</a>
        </div>
    </body>
    </html>
    <?php
    exit;
}