<?php
session_start();
require_once('../../inc/functions.php');

if (!is_logged_in_explizit()) {
    header("Location: " . WEBSITE_BASE . "/community/login.php?error=session_expired");
    exit();
}

$url = STRAPI_API_BASE . '/profiles/me';
$payload = [
    'data' => [
        'scheduledDeletionAt' => null
    ]
];

$result = makeStrapiApiCall('PUT', $url, $payload);

if ($result['success']) {
    $_SESSION['profile']['deleteDate'] = null;
    header("Location: " . WEBSITE_BASE . "/community/privacy-consent/index.php?status=recovered");
    exit();
} else {
    debugLog("Cancel Deletion Error: " . json_encode($result));
    die("Fehler beim Zurücksetzen der Löschung.");
}
?>