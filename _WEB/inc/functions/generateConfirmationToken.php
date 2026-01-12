<?php
// require_once __DIR__ . '/../config.php'; // Nicht benötigt

/**
 * Generiert einen kryptographisch sicheren Token (z.B. für E-Mail-Bestätigung).
 *
 * @return string Ein 32-Zeichen langer Hexadezimal-String (128 Bit Entropie).
 */
function generateConfirmationToken(): string {
    // 16 Bytes an Zufallsdaten (128 Bit)
    // bin2hex wandelt dies in einen 32-Zeichen-String um
    return bin2hex(random_bytes(16));
}