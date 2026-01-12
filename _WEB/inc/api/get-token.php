<?php
session_start();
require_once('../functions.php');
requireLogin(); // prüft JWT und documentId – sonst Abbruch

header('Content-Type: application/json');
echo json_encode(['jwt' => $_SESSION['jwt']]);