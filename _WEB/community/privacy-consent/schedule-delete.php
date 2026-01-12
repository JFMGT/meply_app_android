<?php
session_start();
require_once('../../inc/functions.php');

if (!is_logged_in_explizit()) {
    header("Location: " . WEBSITE_BASE . "/community/login.php?error=session_expired");
    exit();
}

// 14 Tage in der Zukunft
$deletionDate = date('c', strtotime('+14 days'));

$url = STRAPI_API_BASE . '/profiles/me';
$payload = [
    'data' => [
        'scheduledDeletionAt' => $deletionDate
    ]
];

$result = makeStrapiApiCall('PUT', $url, $payload);

if ($result['success']) {
    $_SESSION['profile']['deleteDate'] = $deletionDate;
    header("Location: " . WEBSITE_BASE . "/community/privacy-consent/index.php?status=scheduled_delete");
    exit();
} else {
    debugLog("Schedule Deletion Error: " . json_encode($result));
    die("Fehler beim Planen der Löschung.");
}
?>