document.addEventListener('click', async (e) => {
  const el = e.target.matches('.likeit') ? e.target : e.target.closest('.likeit');
  if (!el) return;
  e.preventDefault();

  const documentId = el.dataset.documentid;
  const contentType = el.dataset.contenttype;
  if (!documentId || !contentType) return;

  const allMatches = document.querySelectorAll(`.likeit[data-documentid="${documentId}"]`);
  allMatches.forEach(btn => btn.classList.add('loading'));

  try {
    const res = await fetch('/inc/api/like-toggle.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ targetDocumentId: documentId, targetType: contentType })
    });

    const result = await res.json();
    if (!res.ok) throw new Error(result.error || 'Unbekannter Fehler beim Liken.');

    // 🧠 Für alle gleichen Beiträge: Zahl und Icon aktualisieren
    allMatches.forEach(btn => {
      const span = btn.querySelector('span');
      let count = parseInt(span?.textContent || '0', 10);

      if (result.status === 'liked') {
        count++;
        btn.innerHTML = `<span>${count}</span> <i class="fa-solid fa-heart"></i>`;
        btn.classList.add('liked');
      } else if (result.status === 'unliked') {
        count = Math.max(count - 1, 0);
        btn.innerHTML = `<span>${count}</span> <i class="fa-regular fa-heart"></i>`;
        btn.classList.remove('liked');
      }
    });
  } catch (err) {
    console.error('Fehler beim Liken:', err);
    notify(err.message || 'Du musst angemeldet sein, um liken zu können.');
  } finally {
    allMatches.forEach(btn => btn.classList.remove('loading'));
  }
});
