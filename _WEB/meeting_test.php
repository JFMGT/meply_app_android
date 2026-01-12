<?php
// ==========================================
// ⚙️ KONFIGURATION
// ==========================================

// Deine Strapi URL (ohne Slash am Ende)
$strapiUrl = 'https://admin.meeplemates.de'; 

// Dein API Token (Erstelle einen unter Settings -> API Tokens)
// WICHTIG: Der Token braucht "find" und "findOne" Rechte für "Meeting"
$apiToken = '0c12d813754c62d9d53619b51b013bc15493350599b9dbaffba5ed3c8ebf80d58a6cbe4ad189084efec134631f29efe13defbac10e9c149836b1b983bc392503c9f69ce3a0d8c2926fc7ffef3a1cfaeb62d90629a1ac539b8808346b30508d0cb398066e96871f58a49ab2b0f422559f0b418b7e29f6d92b75b1d18a22091b0d';

// Die Document ID des problematischen Meetings
$documentId = 'te7wvxuugmzzb6cq83fl78i3';

// ==========================================
// 🚀 LOGIK
// ==========================================

function callStrapi($url, $token, $params = []) {
    $queryString = http_build_query($params);
    $fullUrl = $url . '?' . $queryString;

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $fullUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $token,
        'Content-Type: application/json'
    ]);

    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    return [
        'code' => $httpCode,
        'data' => json_decode($result, true)
    ];
}

// 1. Abruf der PUBLISHED Version (Das sieht dein Frontend)
// In Strapi v5 ist der Standard-Abruf meist "published"
$publishedResponse = callStrapi(
    "$strapiUrl/api/meetings/$documentId", 
    $apiToken, 
    [
        'populate' => 'zipcode',
        'status' => 'published' // Explizit Published anfordern
    ]
);

// 2. Abruf der DRAFT Version (Das siehst du im Admin Panel)
$draftResponse = callStrapi(
    "$strapiUrl/api/meetings/$documentId", 
    $apiToken, 
    [
        'populate' => 'zipcode',
        'status' => 'draft' // Explizit Draft anfordern
    ]
);

// ==========================================
// 🎨 AUSGABE
// ==========================================
?>

<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>Strapi Debugger</title>
    <style>
        body { font-family: sans-serif; padding: 20px; background: #f4f4f4; }
        .box { background: white; padding: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
        h2 { margin-top: 0; }
        .badge { padding: 5px 10px; border-radius: 4px; color: white; font-weight: bold; }
        .green { background: #27ae60; }
        .red { background: #c0392b; }
        .orange { background: #d35400; }
        pre { background: #eee; padding: 10px; overflow-x: auto; }
    </style>
</head>
<body>
    <h1>🔍 Strapi API Diagnose</h1>
    <p>Prüfe Document ID: <strong><?php echo htmlspecialchars($documentId); ?></strong></p>

    <div class="box">
        <h2>1. Frontend Sicht (Published)</h2>
        <?php 
            $pubData = $publishedResponse['data']['data'] ?? null;
            $pubZip = $pubData['zipcode'] ?? null;
        ?>
        
        <?php if ($publishedResponse['code'] !== 200): ?>
            <span class="badge red">API Fehler: <?php echo $publishedResponse['code']; ?></span>
            <pre><?php print_r($publishedResponse['data']); ?></pre>
        <?php elseif (!$pubData): ?>
             <span class="badge orange">Nicht gefunden (evtl. noch gar nicht veröffentlicht?)</span>
        <?php else: ?>
            <p><strong>Meeting Titel:</strong> <?php echo $pubData['title'] ?? 'Kein Titel'; ?></p>
            
            <?php if ($pubZip): ?>
                <span class="badge green">✅ Zipcode vorhanden</span>
                <p>PLZ: <strong><?php echo $pubZip['zip']; ?></strong> (ID: <?php echo $pubZip['id']; ?>)</p>
            <?php else: ?>
                <span class="badge red">❌ Zipcode FEHLT (null)</span>
                <p>Das Frontend sieht keine PLZ!</p>
            <?php endif; ?>
        <?php endif; ?>
    </div>

    <div class="box">
        <h2>2. Backend Sicht (Draft)</h2>
        <?php 
            $draftData = $draftResponse['data']['data'] ?? null;
            $draftZip = $draftData['zipcode'] ?? null;
        ?>

        <?php if ($draftResponse['code'] !== 200): ?>
            <span class="badge red">API Fehler: <?php echo $draftResponse['code']; ?></span>
        <?php elseif (!$draftData): ?>
             <span class="badge red">Draft nicht gefunden</span>
        <?php else: ?>
            <p><strong>Meeting Titel:</strong> <?php echo $draftData['title'] ?? 'Kein Titel'; ?></p>
            
            <?php if ($draftZip): ?>
                <span class="badge green">✅ Zipcode vorhanden</span>
                <p>PLZ: <strong><?php echo $draftZip['zip']; ?></strong> (ID: <?php echo $draftZip['id']; ?>)</p>
            <?php else: ?>
                <span class="badge orange">⚠️ Zipcode fehlt auch im Entwurf</span>
            <?php endif; ?>
        <?php endif; ?>
    </div>

    <div class="box">
        <h2>🧠 Analyse</h2>
        <?php if ($pubZip && $draftZip && $pubZip['id'] == $draftZip['id']): ?>
            <p class="badge green">ALLES OKAY</p>
            <p>Beide Versionen sind synchron. Wenn das Frontend es nicht anzeigt, liegt der Fehler im Frontend-Code (React/Vue/etc.).</p>
        <?php elseif (!$pubZip && $draftZip): ?>
            <p class="badge red">CRITICAL ERROR</p>
            <p><strong>Das "Zombie"-Problem:</strong> Die PLZ existiert im Entwurf, wurde aber beim Erstellen/Veröffentlichen nicht in die Live-Version übernommen. Das bestätigt, dass beim <code>create</code> die Relation verloren geht.</p>
        <?php elseif (!$pubZip && !$draftZip): ?>
            <p class="badge orange">LOGIK FEHLER</p>
            <p>Die PLZ wurde gar nicht gespeichert. Der Fehler liegt in der <code>findZipcodeId</code> Logik oder der Übergabe im Controller.</p>
        <?php endif; ?>
    </div>

</body>
</html>