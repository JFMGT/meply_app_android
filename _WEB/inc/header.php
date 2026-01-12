<?php
//ACHTUNG DOPPELSTRUKTUR: AUCH header_auth.php anpassen!
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}
require_once 'functions.php';
//DAS UMWANDELN DER ABSOLUTEN IN RELATIVE SOLL ZUKÜNFTIG WEGFALLEN. ES SOLL DIREKT AUF DIE ABS. ZUGEGRIFFEN WERDEN:
$baseurl = WEBSITE_BASE;
$adminBaseurl = STRAPI_API_BASE;
// ENDE 
$errorMessage = ''; //Warum hier definiert? 
require_once(INCLUDE_DIR . 'header_data.php');
?>