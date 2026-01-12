async function manageFollow(followId, action) {
  try {
    const res = await fetch('/inc/api/follow_manage.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include', // falls du session brauchst
      body: JSON.stringify({ id: followId, action }),
    });

    const data = await res.json();
    if (res.ok) {
      console.log('Aktion erfolgreich:', data);

      // ✅ Listen neu laden
      await loadFollowLists();
    } else {
      alert(data.error || 'Fehler bei Follow-Aktion');
    }
  } catch (err) {
    console.error('Fehler:', err);
    alert('Verbindungsfehler');
  }
}
