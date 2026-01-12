<?php
// ARCHITEKTUR: session_start() sollte zentral sein
if (session_status() === PHP_SESSION_NONE) session_start();
// ARCHITEKTUR: functions.php (mit esc()) sollte zentral geladen werden
// Wir müssen es hier laden, um esc() nutzen zu können
require_once(__DIR__ . '/../functions.php');
?>
<div id="conversationModal" class="modal hidden message-modal">
  <div class="modal-content message-modal-content">
    <span class="close message-modal-close">&times;</span>

    <div class="message-modal-header">
      <img id="recipientAvatar" class="message-modal-avatar" src="" alt="Avatar" style="display:none;">
      <div class="message-modal-header-text">
        <h2 id="modalTitle" class="message-modal-title">Nachricht schreiben</h2>
        <span id="modalSubtitle" class="message-modal-subtitle"></span>
      </div>
    </div>

    <form id="messageForm" data-user="<?= esc($_SESSION['profile']['documentId'] ?? '') ?>">
      <input type="hidden" name="recipient" id="recipient">
      <input type="hidden" name="reference" id="reference">
      <input type="hidden" name="conversation" id="conversation">

      <label for="message">Nachricht</label>
      <textarea name="message" id="message" required maxlength="500" placeholder="Schreibe deine Nachricht..."></textarea>

      <div class="message-modal-footer">
        <span id="charCounter" class="char-counter">0/500</span>
        <button type="submit" class="message-modal-submit">
          <i class="fa-solid fa-paper-plane"></i> Senden
        </button>
      </div>
    </form>
  </div>
</div>
