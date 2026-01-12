document.addEventListener('DOMContentLoaded', () => {
  const thoughtIcon = document.getElementById('thought-icon');

  // 🧠 Gedanken vom Server laden
  fetch('/inc/api/feed.php')
    .then(res => res.json())
    .then(data => {
      if (!data.feed || !Array.isArray(data.feed)) return;

      const now = Date.now();
      const seenMap = getSeenThoughtMap();

      // ✅ Nur Beiträge aus den letzten 24h behalten
      const recent = data.feed.filter(post => {
        const createdAt = new Date(post.createdAt).getTime();
        return now - createdAt <= 24 * 60 * 60 * 1000;
      });

      // 🧹 Alte Einträge aus der Seen-Map entfernen
      const updatedSeenMap = {};
      for (const [id, timestamp] of Object.entries(seenMap)) {
        if (now - timestamp <= 24 * 60 * 60 * 1000) {
          updatedSeenMap[id] = timestamp;
        }
      }
      saveSeenThoughtMap(updatedSeenMap);

      // 💾 Gedanken lokal speichern (neueste zuerst)
      sessionStorage.setItem('queuedThoughts', JSON.stringify(recent));


      // 🔔 Icon aktualisieren (ggf. roten Punkt anzeigen)
      updateThoughtIcon();
    });

  // 📌 Klick auf Icon → Carousel anzeigen
  if (thoughtIcon) {
    thoughtIcon.addEventListener('click', () => {
      showThoughtCarousel();
    });
  }
});

function getSeenThoughtMap() {
  const raw = sessionStorage.getItem('seenThoughtIds');
  return raw ? JSON.parse(raw) : {};
}

function saveSeenThoughtMap(map) {
  sessionStorage.setItem('seenThoughtIds', JSON.stringify(map));
}

function updateThoughtIcon() {
  const raw = sessionStorage.getItem('queuedThoughts');
  const thoughts = raw ? JSON.parse(raw) : [];
  const seenMap = getSeenThoughtMap();

  const hasUnseen = thoughts.some(t => !seenMap[t.documentId]);

  const icon = document.getElementById('thought-icon');
  if (!icon) return;

  icon.classList.remove('hidden');

  const dot = icon.querySelector('.thought-dot');
  if (dot) {
    dot.style.display = hasUnseen ? 'block' : 'none';
  }
}

function showThoughtCarousel() {
  const raw = sessionStorage.getItem('queuedThoughts');
  if (!raw) return;

  const thoughts = JSON.parse(raw);
  if (!thoughts.length) {
    alert("Keine Gedanken der letzten 24 Stunden vorhanden.");
    return;
  }

  let index = 0;

  const overlay = document.createElement('div');
  overlay.className = 'thought-carousel-overlay';

  const content = document.createElement('div');
  content.className = 'thought-carousel-content';
  overlay.appendChild(content);

  const left = document.createElement('button');
  left.className = 'carousel-arrow left';
  left.innerHTML = '‹';
  left.onclick = () => {
    if (index > 0) {
      index--;
      renderCurrent();
    }
  };

  const right = document.createElement('button');
  right.className = 'carousel-arrow right';
  right.innerHTML = '›';
  right.onclick = () => {
    if (index < thoughts.length - 1) {
      index++;
      renderCurrent();
    }
  };

  overlay.appendChild(left);
  overlay.appendChild(right);

  const close = document.createElement('button');
  close.className = 'carousel-close';
  close.innerHTML = '×';
  close.onclick = () => overlay.remove();
  overlay.appendChild(close);

  function renderCurrent() {
  const thought = thoughts[index];
  if (!thought) return;

  fetch('/inc/api/renderSinglePost.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(thought)
  })
    .then(res => res.text())
    .then(html => {
      content.innerHTML = html;

      // 🔁 Sichtbarkeit der Pfeile aktualisieren
      left.style.display = index > 0 ? 'block' : 'none';
      right.style.display = index < thoughts.length - 1 ? 'block' : 'none';

      // 🧠 Beitrag als gesehen markieren
      const seenMap = getSeenThoughtMap();
      seenMap[thought.documentId] = Date.now();
      saveSeenThoughtMap(seenMap);
      updateThoughtIcon();
    });
}

  renderCurrent();
  document.body.appendChild(overlay);
}
