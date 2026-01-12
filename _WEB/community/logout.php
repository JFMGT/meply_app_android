<?php
logoutUser('/');

function logoutUser($redirectTo = null) {
    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }

    session_unset();
    session_destroy();

    if ($redirectTo) {
        header("Location: $redirectTo");
        exit;
    }
}