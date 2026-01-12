document.addEventListener('DOMContentLoaded', () => {
  // ✅ Event Delegation: Listener auf document.body (existiert IMMER)
  document.body.addEventListener('click', async (event) => {
    // Prüfe ob das geklickte Element (oder ein Parent) .delete-meeting ist
    const button = event.target.closest('.delete-meeting');
    
    if (!button) return; // Kein Delete-Button geklickt
    
    event.preventDefault();
    
    // 1. Hole documentId aus dem data-Attribut
    const documentId = button.dataset.meeting;
    if (!documentId) {
      notify('Meeting-ID fehlt!');
      return;
    }
    
    // 2. Bestätigungsdialog
    if (!confirm('Willst du dieses Meeting wirklich löschen?')) return;
    
    // 3. AJAX-Request an das PHP-Skript
    try {
      const res = await fetch('/inc/api/delete-meeting.php', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ documentId })
      });
      
      const data = await res.json();
      
      if (res.ok) {
        notify('Meeting erfolgreich gelöscht!');
        
        const card = button.closest('.card');
        if (card) {
          // Weiches Ausblenden
          card.style.transition = 'opacity 0.5s ease';
          card.style.opacity = '0';
          
          // Nach Transition aus DOM entfernen
          setTimeout(() => {
            card.remove();
          }, 500);
        }
      } else {
        notify(data.error || 'Fehler beim Löschen.');
      }
    } catch (err) {
      console.error(err);
      notify('Serverfehler beim Löschen.');
    }
  });
});