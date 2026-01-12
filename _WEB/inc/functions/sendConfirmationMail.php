<?php
require_once(__DIR__ . '/../config.php');

/**
 * Sendet die Bestätigungs-E-Mail an einen neuen Benutzer.
 * WRAPPER-ANSATZ: Verlinkt zur PHP verify.php (die dann Strapi aufruft)
 *
 * @param string $to Die E-Mail-Adresse des Empfängers.
 * @param string $token Der Bestätigungs-Token.
 * @return bool True bei Erfolg (Versand versucht), False bei ungültiger E-Mail.
 */
function sendConfirmationMail($to, $token): bool {
    // Validierung
    if (!filter_var($to, FILTER_VALIDATE_EMAIL)) {
        return false;
    }
    
    $subject = "Bitte bestätige deine Registrierung bei Meply.de";
    
    // Verlinkt zur PHP verify.php (saubere URL!)
    $verifyUrl = WEBSITE_BASE . "community/verify?token=" . urlencode($token);
    
    $message = "Hallo!\n\n".
               "vielen Dank für deine Registrierung bei Meply.\n\n".
               "Um deine Anmeldung abzuschließen, klicke bitte auf folgenden Link:\n\n".
               "$verifyUrl\n\n".
               "Solltest du dich nicht registriert haben, ignoriere diese Nachricht einfach.\n\n".
               "Viele Grüße\n".
               "Dein Meply Team";
    
    $headers = "From: " . MAIL_FROM . "\r\n";
    $headers .= "Reply-To: " . MAIL_REPLY_TO . "\r\n";
    $headers .= "X-Mailer: PHP/" . phpversion() . "\r\n";
    $headers .= "MIME-Version: 1.0\r\n";
    $headers .= "Content-Type: text/plain; charset=UTF-8\r\n";
    
    return mail($to, $subject, $message, $headers);
}