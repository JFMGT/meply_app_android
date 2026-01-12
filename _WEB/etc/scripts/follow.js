document.addEventListener('click', async (event) => {
  const btn = event.target.closest('.douserfollow');
  if (!btn) return;

  const documentId = btn.getAttribute('data-document-id');
  if (!documentId){
    console.log("DoFollow");
    return;
  } 

  try {
    const res = await fetch('/inc/api/follow.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ documentId }),
    });

    const data = await res.json();
    console.log('API-Antwort:', data);

    if (data.error) {
      console.error('Fehler von API:', data.error);
      alert('Fehler: ' + (data.error.message || 'Unbekannter Fehler'));
      return;
    }

    if (data.status === 'followed' || data.status === 'unfollowed') {
      if (btn.classList.contains('userfollow')) {
        const icon = btn.querySelector('i');
        const newText = data.status === 'followed' ? 'Entfolgen' : 'Folgen';

        if (icon && icon.nextSibling) {
          icon.nextSibling.textContent = ' ' + newText;
        }

        if (icon) {
          icon.classList.remove(data.status === 'followed' ? 'fa-plus' : 'fa-minus');
          icon.classList.add(data.status === 'followed' ? 'fa-minus' : 'fa-plus');
        }
      } else {
        const parentLi = btn.closest('li');
        if (parentLi) {
          parentLi.remove();
        }
      }
    } else {
      alert(data.error || 'Unbekannter Fehler');
    }
  } catch (err) {
    console.error('Fehler beim Fetch:', err);
    alert('Verbindungsfehler');
  }
});
