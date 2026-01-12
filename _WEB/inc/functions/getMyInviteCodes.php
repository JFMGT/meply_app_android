<?php
/**
 * Holt oder erstellt Invite-Codes für den aktuell eingeloggten User
 * 
 * Diese Funktion verwendet das USER-JWT (nicht den API Token!),
 * da die Route authentifiziert ist und den User aus dem JWT holt.
 * 
 * @param string $userJwt Das JWT des eingeloggten Users (aus Session/Cookie)
 * @return array|false Array mit codes und stats bei Erfolg, false bei Fehler
 */
function getMyInviteCodes($userJwt) {
    $url = STRAPI_API_BASE . "/registration-codes/my-invite-codes";
    
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . $userJwt,  // USER JWT, nicht API Token!
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([])); // Leerer Body
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    
    if (curl_errno($ch)) {
        debugLog("CURL Fehler bei getMyInviteCodes: " . curl_error($ch));
        curl_close($ch);
        return false;
    }
    
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200) {
        debugLog("HTTP Fehler bei getMyInviteCodes: " . $httpCode . " | Response: " . $response);
        return false;
    }
    
    $data = json_decode($response, true);
    
    if (!isset($data['success']) || !$data['success']) {
        debugLog("API Fehler bei getMyInviteCodes: " . json_encode($data));
        return false;
    }
    
    return $data['data'];
}

/**
 * Beispiel-Nutzung im Frontend (z.B. in user-profile.php)
 */
/*
// User-JWT aus Session holen
$userJwt = $_SESSION['user_jwt'] ?? null;

if (!$userJwt) {
    echo "Nicht eingeloggt";
    exit;
}

// Invite-Codes holen/erstellen
$result = getMyInviteCodes($userJwt);

if (!$result) {
    echo "Fehler beim Laden der Codes";
    exit;
}

// Daten sind verfügbar
$codes = $result['codes'];
$stats = $result['stats'];

echo "<h2>Deine Einladungscodes</h2>";
echo "<p>Du hast {$stats['totalCodes']} Codes, davon {$stats['unusedCodes']} unbenutzt.</p>";

if ($stats['newCodesGenerated'] > 0) {
    echo "<p class='success'>🎉 {$stats['newCodesGenerated']} neue Codes wurden für dich erstellt!</p>";
}

if ($stats['canGenerateMore'] > 0) {
    echo "<p>Du kannst noch {$stats['canGenerateMore']} weitere Codes bekommen.</p>";
}

echo "<h3>Deine Codes:</h3>";
echo "<ul>";
foreach ($codes as $code) {
    $status = $code['used'] ? '✓ Verwendet' : '⭘ Verfügbar';
    $usedDate = $code['usedAt'] ? ' am ' . date('d.m.Y', strtotime($code['usedAt'])) : '';
    
    echo "<li>";
    echo "<strong>{$code['code']}</strong> - {$status}{$usedDate}";
    echo "</li>";
}
echo "</ul>";

echo "<p>Limits: Max. {$stats['limits']['maxUnused']} unbenutzte, max. {$stats['limits']['maxTotal']} gesamt</p>";
*/
?>