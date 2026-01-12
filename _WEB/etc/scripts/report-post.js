document.addEventListener('click', async (e) => {
  const btn = e.target.closest('.report-post');
  if (!btn) return;
  e.preventDefault();
  
  const documentId = btn.dataset.documentid;
  const type = btn.dataset.type || 'post';
  const title = btn.dataset.title || 'Inhalt';
  
  if (!documentId) {
    notify('⚠️ documentId fehlt!');
    return;
  }
  
  function escapeHtml(text) {
    return text.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;')
               .replace(/"/g, '&quot;')
               .replace(/'/g, '&#039;');
  }
  
  // Modal erstellen (minimalistisch)
  const overlay = document.createElement('div');
  overlay.style = 'position:fixed; top:0; left:0; width:100vw; height:100vh; background:#0008; display:flex; align-items:center; justify-content:center; z-index:9999;';
  overlay.innerHTML = `
    <div style="background:#fff; padding:1rem; border-radius:8px; max-width:400px; width:80%;">
      <h3>🚩 Melden: ${escapeHtml(title)}</h3>
      <textarea style="width:90%; height:100px;" placeholder="Was ist das Problem?"></textarea>
      <div style="margin-top:10px; text-align:right;">
        <button style="background-color:#fec50d; padding:5px; border:0" class="cancel-report">Abbrechen</button>
        <button style="background-color:#fec50d; padding:5px; border:0" class="submit-report">Melden</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);
  
  // Event-Handler für Buttons
  overlay.querySelector('.cancel-report').onclick = () => overlay.remove();
  
  overlay.querySelector('.submit-report').onclick = async () => {
    const reason = overlay.querySelector('textarea').value.trim();
    if (!reason) return notify('Bitte eine Begründung eingeben.');
    
    try {
      const res = await fetch('/inc/api/post_report.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ documentId, type, reason }),
      });
      
      const result = await res.json();
      overlay.remove();
      
      // Fehlerbehandlung
      if (!res.ok || !result.success) {
        // PHP gibt jetzt: { success: false, error: "Nachricht" }
        notify('⚠️ ' + (result.error || result.message || 'Fehler beim Melden'));
      } else {
        // Erfolg: { success: true, status: "reported", reportId: ... }
        notify('✅ Meldung wurde übermittelt.');
      }
    } catch (error) {
      overlay.remove();
      console.error('Report Error:', error);
      notify('❌ Fehler beim Melden (Netzwerkfehler)');
    }
  };
});