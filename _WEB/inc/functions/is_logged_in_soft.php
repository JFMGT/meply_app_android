<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
$profile = getProfileData();
function is_logged_in_soft(): bool {

    return !empty($_SESSION['jwt']) &&
           !empty($_SESSION['profile']) &&
           isset($_SESSION['profile']['documentId']);
}
?>