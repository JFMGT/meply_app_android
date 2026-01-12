<?php
// ARCHITEKTUR: session_start() sollte zentral sein
if (session_status() === PHP_SESSION_NONE) session_start();
require_once(__DIR__ . '/../config.php'); 
require_once(__DIR__ . '/../functions.php'); 
// HINWEIS: Annahme, dass config.php und functions.php (mit esc())
// bereits von der übergeordneten Datei (z.B. header_auth.php) geladen wurden.
?>
<div id="postModalOverlay" class="modal" style="display: none;">
  <div class="modal-content">
    <div class="modal-button">
    <button id="closeModal">Abbrechen</button> <button id="submitPostBtn">Posten</button>
    </div>
    <?php
$anreize = [
    "Erzähle von dem letzten Spiel, das du gespielt hast – wie war die Runde, mit wem hast du gespielt?",
    "Was war ein besonders schöner Moment bei einem eurer Spieleabende?",
    "Gibt es ein Spiel, das du früher mochtest, aber heute nicht mehr – und warum?",
    "Denk an einen Spieleabend, der dir im Kopf geblieben ist. Was war das Besondere daran?",
    "Hast du ein Spiel, das du immer wieder vorschlägst? Was macht es für dich so besonders?",
    "Erzähle von einem Spiel, bei dem du überrascht warst – im positiven oder negativen Sinn.",
    "Was war dein Einstieg in die Welt der Brettspiele? Wie hat alles angefangen?",
    "Erinnere dich an eine Situation, in der ein Spielabend ganz anders verlief als geplant.",
    "Was ist dir bei einem Spieleabend wichtiger: das Spiel oder die Menschen? Und warum?",
    "Gibt es ein Spiel, das du gerne verschenken würdest? Was steckt dahinter?",
    "Wie würdest du einen perfekten Spieleabend beschreiben – vom Aufbau bis zur letzten Runde?",
    "Was sind deine Gedanken zu Hausregeln oder Spielmodifikationen – nutzt du welche?",
    "Gab es mal eine hitzige Diskussion bei einem Spiel? Wie ist es dazu gekommen?",
    "Welche Knabberei gehört für dich unbedingt zu einem gelungenen Abend – und warum?",
    "Erzähle von einem Spiel, das dich zuerst nicht interessiert hat, dann aber begeistert hat.",
    "Was macht für dich ein Spiel zu einem echten ‚Klassiker‘, den du behalten willst?",
    "Beschreibe ein Spiel, das du nie wieder spielen möchtest – und was dahintersteckt.",
    "Gibt es eine besondere Erinnerung an ein Spiel mit der Familie oder mit Kindern?",
    "Wie findest du Mitspieler:innen, die zu dir passen – worauf achtest du?",
    "Was war dein größter Sieg – oder deine bitterste Niederlage – in einem Spiel?"
];

// Zufällige Auswahl
$placeholder = $anreize[array_rand($anreize)];

// Annahme: $baseUrl kommt von config.php
$baseUrl = defined('WEBSITE_BASE') ? rtrim(WEBSITE_BASE, '/') : '';
?>
    
    <textarea id="postText" placeholder="Idee: <?= esc($placeholder) ?>" rows="4" style="width: 100%;"></textarea>
    <div class="modal-settings"> 
    <input type="text" name="parent-id" style="display:none">
    <div id="previewContainer"></div>
    <input type="file" id="postImage" name="images[]" accept="image/*" multiple style="display: none;" />
    <input type="file" id="postCamera" name="images[]" accept="image/*" capture="environment" multiple style="display: none;" />

    <div class="modal-actions">
    <label for="postImage" class="upload-icon fa-xl" title="Foto aus Galerie wählen">
      <i class="fa-solid fa-image"></i>
    </label>
    <label for="postCamera" class="camera-icon fa-xl" title="Foto aufnehmen">
      <i class="fa-solid fa-camera"></i>
    </label>
  </div>
    <span class="who-can-answer" data-answer="all">Jeder kann diesen Beitrag sehen.</span>
</div>
  </div>
</div>


<div id="altTextModal" class="modal" style="display: none;">
  <div class="modal-content">
    <h4>Alt-Text bearbeiten</h4>
    <img id="altImage" src="" style="max-width:100%; border-radius: 6px;" />
    <input type="text" id="altTextInput" placeholder="Beschreibe das Bild" style="width: 100%; margin-top: 10px;" />
    <button id="saveAltBtn" style="display: none;">Speichern</button>
    
    <label for="saveAltBtn" class="save-icon" >
      <img style="cursor:hover" src="<?= esc($baseUrl) ?>/etc/images/save.svg">
    </label>

  </div>
</div>