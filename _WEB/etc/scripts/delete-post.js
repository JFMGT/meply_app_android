document.addEventListener('click', async (e) => {
  const btn = e.target.closest('.delete-post');
  if (!btn) return;

  const documentId = btn.dataset.documentid;
  if (!documentId) return;

  if (!confirm('Diesen Beitrag wirklich löschen?')) return;

  try {
    // ✅ documentId als GET-Parameter übergeben
    const res = await fetch('/inc/api/post_delete.php?documentId=' + encodeURIComponent(documentId), {
      method: 'DELETE', // 🟡 POST bleibt erhalten
      credentials: 'include'
    });

    const result = await res.json();

    if (!res.ok) {
      console.warn('❌ Fehler vom Server:', result);
      notify(result.error || 'Beitrag konnte nicht gelöscht werden.');
      return;
    }

    if (result.status === 'deleted') {
      notify('✅ Beitrag dauerhaft gelöscht');
      btn.closest('.post')?.remove();
    } else if (result.status === 'soft-deleted') {
      notify('🔕 Beitrag wurde entleert (Antworten vorhanden)');
      const postEl = btn.closest('.post');
      if (postEl) {
        postEl.querySelector('.feed-content')?.replaceChildren('[gelöschter Beitrag]');
        btn.remove(); // optional: Löschen-Button entfernen
      }
    } else {
      notify('⚠️ Unerwartete Antwort vom Server');
      console.log(result);
    }

  } catch (err) {
    console.error('Fehler beim Löschen:', err);
    notify('Netzwerkfehler oder Server nicht erreichbar.');
  }
});