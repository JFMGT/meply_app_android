
document.addEventListener('DOMContentLoaded', () => {
  let modalLoaded = false;
  let modal, form, closeBtn, convoField, messageTextarea, charCounter, modalTitle, recipientAvatar;
  let messageContainer = null;
  let formSubmitBound = false; // verhindert mehrfaches Binden

  async function loadModal() {
    if (modalLoaded) return;
    const res = await fetch('/inc/message/message-modal.php');
    const html = await res.text();
    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);
    modalLoaded = true;

    modal = document.getElementById('conversationModal');
    form = document.getElementById('messageForm');
    closeBtn = modal.querySelector('.close');
    convoField = form.querySelector('[name="conversation"]');
    messageTextarea = document.getElementById('message');
    charCounter = document.getElementById('charCounter');
    modalTitle = document.getElementById('modalTitle');
    recipientAvatar = document.getElementById('recipientAvatar');

    // Zeichenzähler Event
    if (messageTextarea && charCounter) {
      messageTextarea.addEventListener('input', () => {
        const length = messageTextarea.value.length;
        charCounter.textContent = `${length}/500`;

        // Farbe ändern bei fast voll
        if (length > 450) {
          charCounter.style.color = '#ff4444';
        } else if (length > 400) {
          charCounter.style.color = '#ffaa00';
        } else {
          charCounter.style.color = '#999';
        }
      });
    }

    // Schließen beim Close-Button
    closeBtn.addEventListener('click', () => {
      if (modal) modal.classList.add('hidden');
    });

    // Schließen beim Klick außerhalb
    document.addEventListener('click', (e) => {
      if (modal && !modal.classList.contains('hidden') && e.target === modal) {
        modal.classList.add('hidden');
      }
    });

    // Nur einmal Submit-Handler registrieren
    if (!formSubmitBound) {
      form.addEventListener('submit', async (ev) => {
        ev.preventDefault();

        const submitBtn = form.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn.innerHTML;

        // Loading State
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Wird gesendet...';

        const payload = {
          recipient: form.recipient.value,
          reference: form.reference.value,
          message: form.message.value
        };

        if (convoField && convoField.value) {
          payload.conversationId = convoField.value;
        }

        try {
          const res = await fetch('/inc/api/send-msg.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
          });

          if (!res.ok) throw new Error('Senden fehlgeschlagen');

          notify('Nachricht gesendet!');

          // Wenn Nachrichtenbereich aktiv ist: Nachricht direkt anhängen
          if (messageContainer) {
            const messageEl = document.createElement('div');
            messageEl.className = 'message sent';
            messageEl.innerHTML = `
              <div class="msg-text">${payload.message}</div>
              <div class="msg-time">${new Date().toLocaleString('de-DE')}</div>
            `;
            messageContainer.appendChild(messageEl);
            messageContainer.scrollTop = messageContainer.scrollHeight;
          }

          // Modal entfernen und Reset
          if (modal) {
            modal.remove();
            modal = null;
            form = null;
            closeBtn = null;
            convoField = null;
            formSubmitBound = false; // wichtig!
            modalLoaded = false;
          }

        } catch (err) {
          notify('Fehler: ' + err.message);
          // Reset Button
          submitBtn.disabled = false;
          submitBtn.innerHTML = originalBtnText;
        }
      });

      formSubmitBound = true;
    }
  }

  document.addEventListener('click', async (e) => {
    const link = e.target.closest('.conversation');
    if (!link) return;

    e.preventDefault();

    const wrapper = link.closest('.pm-messages-wrapper');
    messageContainer = wrapper ? wrapper.querySelector('.pm-messages') : null;

    await loadModal();

    form.reset();

    const recipient = link.dataset.recipient || '';
    const recipientName = link.dataset.recipientName || '';
    const recipientAvatarUrl = link.dataset.recipientAvatar || '';
    const reference = link.dataset.reference || '';
    const conversation = link.dataset.conversation || '';

    form.recipient.value = recipient;
    form.reference.value = reference;
    if (convoField) {
      convoField.value = conversation;
    }

    // Titel und Avatar anpassen
    if (recipientName) {
      modalTitle.innerHTML = `Nachricht an <strong>${recipientName}</strong>`;
    } else {
      modalTitle.textContent = 'Nachricht schreiben';
    }

    // Avatar anzeigen
    if (recipientAvatarUrl && recipientAvatar) {
      recipientAvatar.src = recipientAvatarUrl;
      recipientAvatar.style.display = 'block';
      recipientAvatar.alt = `Avatar von ${recipientName}`;
    } else if (recipientAvatar) {
      recipientAvatar.style.display = 'none';
    }

    // Zeichenzähler zurücksetzen
    if (charCounter) {
      charCounter.textContent = '0/500';
      charCounter.style.color = '#999';
    }

    // Auto-Focus auf Textarea
    setTimeout(() => {
      if (messageTextarea) messageTextarea.focus();
    }, 100);

    modal.classList.remove('hidden');
  });
});

