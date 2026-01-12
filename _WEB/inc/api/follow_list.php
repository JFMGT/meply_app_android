<?php

// -----------------------------------------------------------------
// KORREKTUR: Lade die ZENTRALE functions.php
// -----------------------------------------------------------------
// Diese Datei lädt config.php, api-helpers.php (mit makeStrapiApiCall)
// UND checkFollowStatus.php
require_once('../functions.php'); 

//requireLogin();
header('Content-Type: application/json');
// -----------------------------------------------------------------

// ALT (falsch):
// require_once('../config.php');
// include("../functions/checkFollowStatus.php"); // <-- Lädt den Helfer NICHT

// Der Rest deines Codes funktioniert jetzt, da alle Funktionen geladen sind:
$pendingFollowers = checkFollowStatus('all', 'current', 'pending');
$acceptedFollowers = checkFollowStatus('all', 'current', 'accepted');
$following = checkFollowStatus('current', 'all', 'accepted');
$blocked = checkFollowStatus('all', 'current', 'declined');

echo json_encode([
    'pending' => $pendingFollowers,
    'followers' => $acceptedFollowers,
    'following' => $following,
    'blocked' => $blocked
]);