<?php
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit etc.

// CSRF-Token validieren
requireCsrfToken();

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode([
        'success' => false,
        'error'   => 'Nicht eingeloggt',
        'message' => 'Du musst angemeldet sein, um zu melden.'
    ]);
    exit;
}

// --- 2. Eingabe holen und validieren ---
$input = json_decode(file_get_contents('php://input'), true);
$documentId = $input['documentId'] ?? null;
$type = $input['type'] ?? 'post';
$reason = $input['reason'] ?? 'standard';

// Validierung
if (empty($documentId)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error'   => 'Fehlende Eingabe',
        'message' => 'documentId fehlt oder ist ungültig.'
    ]);
    exit;
}

// --- 3. API-Aufruf ---
$strapiUrl = STRAPI_API_BASE . '/post-report';
$payload = [
    'targetDocumentId' => $documentId,
    'targetType' => $type,
    'reason' => $reason
];

$result = makeStrapiApiCall('POST', $strapiUrl, $payload);

// --- 4. Konsistente Antwort ---
if ($result['success']) {
    // Erfolg
    http_response_code($result['code']);
    $responseBody = $result['response'] ?? [];
    if (is_array($responseBody)) {
        $responseBody['success'] = true;
    }
    echo json_encode($responseBody ?: ['success' => true]);
} else {
    // Fehlerfall
    
    // DEBUG (kann später entfernt werden)
    error_log("=== STRAPI ERROR DEBUG ===");
    error_log("HTTP Code: " . $result['code']);
    error_log("Response Type: " . gettype($result['response']));
    error_log("Response: " . print_r($result['response'], true));
    error_log("=========================");
    
    $errorCode = $result['code'] ?: 500;
    http_response_code($errorCode);
    
    debugLog("Post Report Error: ID {$documentId}, HTTP {$result['code']} - " . json_encode($result));
    
    // Fehlertext extrahieren - verschiedene Formate abdecken
    $errorMsg = 'Unbekannter Fehler beim Melden';
    
    if (is_array($result['response'])) {
        // Strapi v5 Error Formate:
        $errorMsg = $result['response']['message']           // Standard
                 ?? $result['response']['error']['message']  // Nested
                 ?? $result['response']['error']             // Error als String
                 ?? $errorMsg;
    } elseif (is_string($result['response'])) {
        $errorMsg = $result['response'];
    } elseif ($errorCode === 0) {
        $errorMsg = 'Netzwerkfehler';
    }
    
    echo json_encode([
        'success' => false,
        'error'   => $errorMsg,
        'code'    => $result['response']['code'] ?? null
    ]);
}
?>