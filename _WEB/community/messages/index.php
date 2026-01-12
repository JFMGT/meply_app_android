<?php 
// ARCHITEKTUR: header_auth.php lädt hoffentlich config.php und startet session.
include('../../inc/header_auth.php'); 

$jwt = $_SESSION['jwt'] ?? null;
// ARCHITEKTUR-HINWEIS: getProfileData() sollte idealerweise in functions.php sein.
// Wir gehen davon aus, dass es hier verfügbar ist.
$profileData = getProfileData(); 
$myProfileId = $_SESSION['profile']['documentId'] ?? null;

// --- API-Abfrage (Manuelles cURL beibehalten) ---
$ch = curl_init(STRAPI_API_BASE .'/conversations/me');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Accept: application/json"
]);
$response = curl_exec($ch);
curl_close($ch);

if (!$response) {
    echo "<p>Fehler: Kein API-Response.</p>";
    // include footer hier wäre nett, aber wir halten uns ans Original
    exit;
}

$data = json_decode($response, true);

if (json_last_error() !== JSON_ERROR_NONE) {
    // SICHERHEIT: json_last_error_msg() ist sicher, aber $response könnte Rohdaten enthalten.
    // Besser nicht die komplette Roh-Antwort im Fehlerfall ausgeben (Info Disclosure).
    echo "<p>Fehler beim Parsen der Server-Antwort.</p>";
    // echo "<pre>$response</pre>"; // ENTFERNT: Info Disclosure Risiko
    exit;
}

// Optional: Notifications archivieren
if (function_exists('archiveNotificationsByType')) {
    archiveNotificationsByType("pm");
}

$conversations = $data ?? [];
?>

<div class="content content-plain top-margin">
  <h1>Deine Nachrichten</h1>

  <div class="pm-overview">
    <?php 
    if (empty($conversations)) {
         echo "<div class='card'><p>Keine Nachrichten vorhanden.</p></div>";
    } else {
        foreach ($conversations as $conv):
            $partner = null;
            if (!empty($conv['participants']) && is_array($conv['participants'])) {
                foreach ($conv['participants'] as $p) {
                    if (($p['documentId'] ?? null) !== $myProfileId) {
                        $partner = $p;
                        break;
                    }
                }
            }

            // --- SICHERHEIT (XSS-FIXES): Alles escapen! ---
            // Wir nutzen htmlspecialchars direkt, falls keine esc() Funktion verfügbar ist.
            $partnerSlug = htmlspecialchars($partner['userslug'] ?? '', ENT_QUOTES, 'UTF-8');
            $partnerName = htmlspecialchars($partner['username'] ?? 'Unbekannt', ENT_QUOTES, 'UTF-8');
            $partnerDocumentId = htmlspecialchars($partner['documentId'] ?? '', ENT_QUOTES, 'UTF-8');

            // Avatar für Modal vorbereiten
            $partnerAvatar = '';
            if (!empty($partner['avatar'][0]['url'])) {
                $partnerAvatar = htmlspecialchars(STRAPI_IMAGE_BASE . $partner['avatar'][0]['url'], ENT_QUOTES, 'UTF-8');
            } elseif (!empty($partner['id'])) {
                $hash = crc32($partner['id']);
                $index = ($hash % 8) + 1;
                $partnerAvatar = htmlspecialchars(WEBSITE_BASE . "/etc/images/avatar{$index}.png", ENT_QUOTES, 'UTF-8');
            } else {
                $partnerAvatar = htmlspecialchars(WEBSITE_BASE . "/etc/images/avatar1.png", ENT_QUOTES, 'UTF-8');
            }
            
            $meeting = $conv['meeting'] ?? [];
            // $related = $conv['relatedRequest']['title'] ?? null; // Unused

            $eventLocation = '';
            $eventName = $meeting['event']['Title'] ?? null;
            $locationName = $meeting['location']['Titel'] ?? null;

            if ($eventName && $locationName) {
                $eventLocation = "Event: " . htmlspecialchars($eventName, ENT_QUOTES, 'UTF-8') . " – " . htmlspecialchars($locationName, ENT_QUOTES, 'UTF-8');
            } elseif ($eventName) {
                $eventLocation = "Event: " . htmlspecialchars($eventName, ENT_QUOTES, 'UTF-8');
            } elseif ($locationName) {
                $eventLocation = "Location: " . htmlspecialchars($locationName, ENT_QUOTES, 'UTF-8');
            }

            if (!empty($meeting['date'])) {
                try {
                    $dt = new DateTime($meeting['date']);
                    $formattedDate = $dt->format('d.m.Y, H:i \U\h\r');
                    if ($eventLocation) {
                         $eventLocation .= " am " . $formattedDate;
                    } else {
                         $eventLocation = "Datum: " . $formattedDate;
                    }
                } catch (Exception $e) { }
            }
            if (empty($eventLocation)) {
                $eventLocation = "Direktnachricht";
            }

            $timestamp = 'Unbekannt';
            if (!empty($conv['lastMessageAt'])) {
                 try { $timestamp = date('d.m.Y H:i', strtotime($conv['lastMessageAt'])); } catch (Exception $e) {}
            }

            // KRITISCHE XSS-FIXES für data-Attribute und Ausgabe
            $safeTitle = htmlspecialchars($meeting['title'] ?? '', ENT_QUOTES, 'UTF-8');
            $safeDescription = htmlspecialchars($meeting['description'] ?? '', ENT_QUOTES, 'UTF-8');
            $safeMeetingId = htmlspecialchars($meeting['documentId'] ?? '', ENT_QUOTES, 'UTF-8');
            $safeConvoId = htmlspecialchars($conv['id'] ?? $conv['documentId'] ?? '', ENT_QUOTES, 'UTF-8'); // Fallback auf documentId falls id fehlt

            $unreadClass = !empty($conv['hasUnread']) ? ' pm-card-unread' : '';

            // HTML sicher ausgeben
            echo "<div class='pm-card{$unreadClass}'
                       data-convo-id='{$safeConvoId}'
                       data-title='{$safeTitle}'
                       data-meeting-id='{$safeMeetingId}'
                       data-description='{$safeDescription}'
                       data-recipient='{$partnerDocumentId}'
                       data-recipient-name='{$partnerName}'
                       data-recipient-avatar='{$partnerAvatar}'>";

            echo "<div class='pm-header clickable'>";
            // Link zum Partner sicher bauen
            $partnerLink = '#';
            if ($partnerSlug) {
                 // Annahme: WEBSITE_BASE ist sicher (aus config)
                 $partnerLink = htmlspecialchars(WEBSITE_BASE . "/user/" . $partnerSlug, ENT_QUOTES, 'UTF-8');
            }

            echo "<div class='pm-head-content'>
                    <a style='text-decoration:none; color:#fec50d; margin-right:5px' href='{$partnerLink}'>
                        <i class='fa-solid fa-user'></i> {$partnerName}
                    </a> 
                    <span class='pm-meta'> <i class='fa-solid fa-calendar'></i> {$timestamp}</span><br>
                    <span class='pm-meta'>{$eventLocation}</span><br> <br> 
                    Gesuch: {$safeTitle} <br>{$safeDescription}
                  </div>";
            echo "<div class='pm-meta pm-toggle pm-delete'><i class='fa-solid fa-trash'></i></div>";
            echo "</div>"; // pm-header
            echo "</div>"; // pm-card

        endforeach;
    }
    ?>
  </div>

  <div class="pm-conversation-view" style="display:none">
    <div class="pm-header-view">
      <a href="#" class="pm-back">← Zurück</a>
    </div>
    <div class="pm-active-header pm-card">
      </div>
    <div class="full-meeting-card"></div>
    <div class="pm-messages-wrapper pm-card">
      <div class="pm-messages"></div>
      <div class="pm-actions">
        <a href="#" class="conversation send-message-button btn-reply"><i class="fa-solid fa-envelope"></i> Antworten</a>
      </div>
    </div>
  </div>
</div>

<style>
  .pm-card { background-color: #151d26; border-radius: 12px; margin-bottom: 1rem; overflow: hidden; box-shadow: 0 4px 8px rgba(0,0,0,0.3); transition: all 0.3s ease; padding: 1rem; }
  .pm-card-unread{ border-left: 35px solid #fec50d; }
  .pm-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; padding: 1rem; border-radius: 12px; }
  .pm-meta { color: #bbb; font-size: 0.9rem; }
  .pm-conversation-view { margin-top: 2rem; }
  .pm-header-view { display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem; }
  .pm-back { color: #f1c40f; text-decoration: none; font-weight: bold; }
  .pm-active-header { margin-bottom: 1rem; }
  .pm-messages { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem; }
  .message { max-width: 80%; padding: 0.5rem 0.75rem; border-radius: 10px; font-size: 0.95rem; line-height: 1.4; word-break: break-word; display: flex; flex-direction: column; }
  .message.received { background-color: #4a4c6a; align-self: flex-start; color: #fff; }
  .no-reply-warning{ background-color: #fec50d !important; color:black !important; }
  .message.sent { background-color: #60628a; align-self: flex-end; color: #fff; }
  .pm-messages .meta{ background-color:#fec50d; margin:0 auto; width:100%; color:black; line-height: 1em; }
  .msg-time { font-size: 0.75rem; color: #ccc; margin-top: 0.25rem; align-self: flex-end; }
  .btn-reply { background: #f1c40f; color: #000; padding: 0.5rem 1rem; border-radius: 6px; text-decoration: none; }
</style>

<script>
// ... (Ihr gesamter JavaScript-Block bleibt hier unverändert stehen) ...
// ... (Stellen Sie sicher, dass Sie den JS-Code aus Ihrer vorherigen Nachricht hier einfügen) ...
document.addEventListener('DOMContentLoaded', () => {
  const overview = document.querySelector('.pm-overview');
  const view = document.querySelector('.pm-conversation-view');
  const backBtn = document.querySelector('.pm-back');
  const messagesContainer = document.querySelector('.pm-messages');
  const replyButton = document.querySelector('.btn-reply');
  const activeHeader = document.querySelector('.pm-active-header');

  let reloadInterval = null;
  let countdownInterval = null;
  let currentConversationId = null;
  let countdown = 60;

  let controlBar = null;
  let refreshIcon = null;
  let toggleIcon = null;
  let countdownText = null;

  async function reloadMessages() {
    if (!currentConversationId || view.style.display === 'none') return;

    try {
      const res = await fetch(`/inc/api/load-messages.php?id=${encodeURIComponent(currentConversationId)}`);
      // HIER: Optional auf res.ok prüfen, aber Ihr PHP liefert HTML auch bei Fehlern
      const html = await res.text();

      // Referenznachricht erhalten
      const meta = messagesContainer.querySelector('.message.meta');
      messagesContainer.innerHTML = '';
      if (meta) messagesContainer.appendChild(meta);

      const temp = document.createElement('div');
      temp.innerHTML = html;
      [...temp.children].forEach(child => messagesContainer.appendChild(child));

      countdown = 60;
      if (countdownText) {
        countdownText.textContent = `Aktualisierung in ${countdown}s`;
      }
    } catch (err) {
      console.warn('Nachrichten konnten nicht neu geladen werden:', err.message);
    }
  }

  function createControlBar() {
    if (controlBar) return; // Schon vorhanden

    controlBar = document.createElement('div');
    controlBar.className = 'pm-countdown';
    controlBar.style.fontSize = '0.8rem';
    controlBar.style.color = '#bbb';
    controlBar.style.marginTop = '0.5rem';
    controlBar.style.textAlign = 'right';
    controlBar.style.display = 'flex';
    controlBar.style.justifyContent = 'flex-end';
    controlBar.style.alignItems = 'center';
    controlBar.style.gap = '10px';

    refreshIcon = document.createElement('i');
    refreshIcon.className = 'fa-solid fa-rotate';
    refreshIcon.title = 'Manuell aktualisieren';
    refreshIcon.style.cursor = 'pointer';

    toggleIcon = document.createElement('i');
    toggleIcon.className = 'fa-solid fa-play';
    toggleIcon.title = 'Auto-Aktualisierung starten';
    toggleIcon.style.cursor = 'pointer';

    countdownText = document.createElement('span');
    countdownText.style.display = 'none';

    refreshIcon.addEventListener('click', reloadMessages);

    toggleIcon.addEventListener('click', () => {
      if (reloadInterval) {
        stopAutoReload();
      } else {
        startAutoReload();
      }
    });

    controlBar.appendChild(refreshIcon);
    controlBar.appendChild(toggleIcon);
    controlBar.appendChild(countdownText);

    messagesContainer.parentElement.appendChild(controlBar);
  }

  function startAutoReload() {
    stopAutoReload();
    countdown = 60;
    countdownText.style.display = 'inline';
    toggleIcon.className = 'fa-solid fa-pause';
    toggleIcon.title = 'Auto-Aktualisierung pausieren';

    reloadInterval = setInterval(reloadMessages, 60000);
    countdownInterval = setInterval(() => {
      countdown--;
      if (countdown <= 0) countdown = 60;
      countdownText.textContent = `Aktualisierung in ${countdown}s`;
    }, 1000);
  }

  function stopAutoReload() {
    clearInterval(reloadInterval);
    clearInterval(countdownInterval);
    reloadInterval = null;
    countdownInterval = null;
    if (countdownText) countdownText.style.display = 'none';
    if (toggleIcon) {
      toggleIcon.className = 'fa-solid fa-play';
      toggleIcon.title = 'Auto-Aktualisierung starten';
    }
  }

document.addEventListener('click', async function (event) {
  const deleteEl = event.target.closest('.pm-delete');
  if (!deleteEl) return;

  event.stopPropagation();

  const card = deleteEl.closest('.pm-card');
  if (!card) return;

  const convoId = card.dataset.convoId;
  if (!convoId) {
    console.error('Keine Conversation-ID gefunden');
    return;
  }

  const confirmed = confirm('Möchtest du diese Konversation wirklich löschen? Dein Gegenüber wird die über diesen Kanal nicht mehr Antworten können.');
  if (!confirmed) return;

  try {
    const response = await fetch('/inc/api/deleteConversation.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ convoId })
    });

    const data = await response.json();

    if (data.success) {
      card.remove();
      console.log('Konversation gelöscht');
    } else {
      console.error('Fehler beim Löschen:', data.message || 'Unbekannter Fehler');
    }
  } catch (err) {
    console.error('Netzwerkfehler:', err);
  }
});


  document.querySelectorAll('.pm-card .clickable').forEach(header => {
    header.addEventListener('click', async (event) => {
      // Wenn auf den Löschbereich oder dessen Kinder geklickt wurde → NICHT öffnen
      if (event.target.closest('.pm-delete')) return;
      const card = header.closest('.pm-card');
      const convoId = card.dataset.convoId;
      const meetingId = card.dataset.meetingId;
      console.log(card.dataset);
      const clonedHeader = card.querySelector('.pm-head-content').cloneNode(true);
      card.classList.remove('pm-card-unread');
      overview.style.display = 'none';
      view.style.display = 'block';
      messagesContainer.innerHTML = '';
      activeHeader.innerHTML = '';
      activeHeader.appendChild(clonedHeader);
      replyButton.setAttribute('data-conversation', convoId);
      replyButton.setAttribute('data-recipient', card.dataset.recipient || '');
      replyButton.setAttribute('data-recipient-name', card.dataset.recipientName || '');
      replyButton.setAttribute('data-recipient-avatar', card.dataset.recipientAvatar || '');

      // Referenznachricht einfügen
      const title = card.getAttribute('data-title');
      const description = card.getAttribute('data-description');
      

      try {
        const res = await fetch(`/inc/api/load-messages.php?id=${encodeURIComponent(convoId)}`);
        if (!res.ok) throw new Error('Fehler beim Laden');
        const html = await res.text();

        const temp = document.createElement('div');
        temp.innerHTML = html;
        [...temp.children].forEach(child => messagesContainer.appendChild(child));

        stopAutoReload();
        createControlBar();
      } catch (err) {
        messagesContainer.innerHTML = '<p style="color:red">Nachrichten konnten nicht geladen werden.</p>';
      }

      const meetingContainer = document.querySelector('.full-meeting-card');
if (meetingId && meetingContainer) {
  try {
    const res = await fetch(`/inc/api/getMeeting.php?id=${encodeURIComponent(meetingId)}`);
    if (!res.ok) throw new Error('Fehler beim Laden der Anzeige');
    const html = await res.text();
    const temp = document.createElement('div');
temp.innerHTML = html;

// Alle .conversation.send-message-button entfernen
temp.querySelectorAll('.conversation.send-message-button').forEach(el => el.remove());

meetingContainer.innerHTML = temp.innerHTML;  } catch (err) {
    meetingContainer.innerHTML = '<p style="color:red">Anzeige konnte nicht geladen werden.</p>';
  }
} else {
  // meetingId ist leer oder Container nicht gefunden → einfach leer lassen
  if (meetingContainer) {
    meetingContainer.innerHTML = '';
  }
}


    });
  });

  backBtn.addEventListener('click', (e) => {
    e.preventDefault();
    stopAutoReload();
    currentConversationId = null;
    view.style.display = 'none';
    overview.style.display = 'block';
    messagesContainer.innerHTML = '';
    activeHeader.innerHTML = '';
    if (controlBar) {
      controlBar.remove();
      controlBar = null;
      refreshIcon = null;
      toggleIcon = null;
      countdownText = null;
    }
  });



  // Seite verlassen → Auto-Reload stoppen
  window.addEventListener('beforeunload', () => {
    stopAutoReload();
  });
});
</script>

<?php include('../../inc/footer.php'); ?>