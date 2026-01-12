document.addEventListener('click', function(event) {
  const target = event.target.closest('.delete-message');
  if (!target) return;

  const documentId = target.dataset.id;

  if (!documentId) {
    console.error('Keine documentId im Button gefunden.');
    return;
  }

  if (!confirm('Möchtest du diese Nachricht wirklich löschen?')) return;

  fetch('/inc/api/deleteMessage.php', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ documentId })
  })
  .then(res => res.json())
  .then(data => {
    if (data.success) {
      console.log('Nachricht gelöscht');
      // Optional: Nachricht im UI als gelöscht markieren
      const msgEl = target.closest('.message');
      if (msgEl) {
        msgEl.classList.add('deleted');
        msgEl.innerHTML = '<em>Diese Nachricht wurde gelöscht.</em>';
      }
    } else {
      console.error('Fehler:', data.message || 'Unbekannter Fehler');
    }
  })
  .catch(err => {
    console.error('Netzwerk-/Serverfehler:', err);
  });
});
