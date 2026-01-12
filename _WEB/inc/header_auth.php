<?php
//ACHTUNG DOPPELSTRUKTUR: AUCH header.php anpassen!
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}
require_once 'config.php';
require_once 'functions.php';
//DAS UMWANDELN DER ABSOLUTEN IN RELATIVE SOLL ZUKÜNFTIG WEGFALLEN. ES SOLL DIREKT AUF DIE ABS. ZUGEGRIFFEN WERDEN:
$baseurl = WEBSITE_BASE;
$adminBaseurl = STRAPI_API_BASE;
// ENDE
$errorMessage = ''; //Warum hier definiert? 


if (!is_logged_in_soft()) {
    header("Location: " . WEBSITE_BASE . "/community/login.php");
    exit();
}


// 🔹 Nach erfolgreichem Login prüfen, ob Datenschutz-Zustimmung aktuell ist
$currentVersion = getCurrentPrivacyVersion();
$userVersion = $_SESSION['profile']['lastAcceptedPrivacyVersion'] ?? null;

// Schutz: nur prüfen, wenn User eingeloggt ist UND Profil geladen ist
if (isset($currentVersion, $userVersion)) {
    if (trim((string)$currentVersion) !== trim((string)$userVersion)) {
        $_SESSION['needs_consent'] = true;

        if (basename($_SERVER['PHP_SELF']) !== 'privacy-consent.php') {
            header("Location: " . WEBSITE_BASE . "/community/privacy-consent/");
            exit();
        }
    } else {
        unset($_SESSION['needs_consent']);
    }
}



require_once(INCLUDE_DIR . 'header_data.php');
?>