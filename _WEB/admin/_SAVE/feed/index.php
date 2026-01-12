<?php 
include('../../inc/header_auth.php');
?>
 
<div class="content">
    <div class="feed">
    </div>    
     <div class="thread-view" style="display:none;">
    <button class="back-to-feed"><img src="<?= $baseUrl ?>/etc/images/back_arrow.png"> Post</button>
    <div class="thread-content"></div>
  </div>
            
</div>
<div id="lightbox" class="lightbox" onclick="closeLightbox()">
  <span class="close-btn" onclick="closeLightbox()">&times;</span>
  <img class="lightbox-image" src="" alt="">
  <div class="lightbox-caption"></div>
</div>

 <script>
function openLightbox(url, alt) {
  const box = document.getElementById('lightbox');
  const img = box.querySelector('.lightbox-image');
  img.src = url;
  img.alt = alt;
  box.querySelector('.lightbox-caption').textContent = alt;
  box.classList.add('visible');
}

function closeLightbox() {
  const box = document.getElementById('lightbox');
  box.classList.remove('visible');
  box.querySelector('.lightbox-image').src = '';
}

// ESC-Taste zum Schließen
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeLightbox();
});


document.addEventListener('DOMContentLoaded', () => {
  const feedEl = document.querySelector('.feed');
  const threadEl = document.querySelector('.thread-view');
  const threadContentEl = threadEl.querySelector('.thread-content');
  const backBtn = threadEl.querySelector('.back-to-feed');

  // 🧭 Direkt beim Laden Feed laden
  loadFeed();

  // 🔁 Feed laden + Event-Handler setzen
  function loadFeed() {
    fetch('../../inc/api/feed.php')
      .then(res => res.text())
      .then(html => {
        feedEl.innerHTML = html;

        // 🧠 Event Listener für "Antworten anzeigen"
        document.querySelectorAll('.show-replys').forEach(link => {
          link.addEventListener('click', e => {
            e.preventDefault();
            const postId = e.target.dataset.documentid;
            if (postId) {
              showThread(postId);
            }
          });
        });
      })
      .catch(err => {
        feedEl.innerHTML = '<p class="error">❌ Fehler beim Laden des Feeds</p>';
        console.error(err);
      });
  }

  // 🔄 Thread-Ansicht anzeigen
  function showThread(documentId) {
    feedEl.style.display = 'none';
    threadEl.style.display = 'block';
    threadContentEl.innerHTML = '<p>Lade Diskussion...</p>';

    // ⏪ Verlaufseintrag setzen
    history.pushState({ view: 'thread' }, '', '#thread-' + documentId);

    fetch('../../inc/api/thread.php?documentId=' + encodeURIComponent(documentId))
      .then(res => res.text())
      .then(html => {
        threadContentEl.innerHTML = html;
      })
      .catch(err => {
        threadContentEl.innerHTML = '<p class="error">❌ Fehler beim Laden</p>';
        console.error(err);
      });
  }

  // 🔙 Klick auf "Zurück zum Feed"
  backBtn.addEventListener('click', () => {
    showFeed();
    history.pushState({ view: 'feed' }, '', '/');
  });

  // ⏮️ Browser-Zurück-Button abfangen
  window.addEventListener('popstate', (event) => {
    if (event.state?.view === 'thread') {
      showFeed();
    } else {
      showFeed();
    }
  });

  // 🔁 Feed wieder einblenden
  function showFeed() {
    threadEl.style.display = 'none';
    feedEl.style.display = 'flex';
  }

  // 💡 Falls andere Skripte loadFeed() brauchen:
  window.loadFeed = loadFeed;
  window.showThread = showThread;
});

</script>

    
<style>
  .feed, .thread-view {
    margin-top:100px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 600px;
  width:100%;
}

.post {
  border: 1px solid #ccc;
  padding: 1rem;
  border-radius: 8px;
  background: #fff;
  position: relative;
}

.post .icon-bar{
  position: absolute;
  right:10px;
  top:10px;
}

.post .meta {
  font-size: 0.9em;
  color: #666;
  margin-bottom: 0.5rem;
}

</style>
<script>
function nextSlide(id) {
  const slider = document.getElementById(id);
  const slides = slider.querySelectorAll('.slide');
  const activeIndex = Array.from(slides).findIndex(s => s.classList.contains('active'));
  if (activeIndex !== -1) {
    slides[activeIndex].classList.remove('active');
    const nextIndex = (activeIndex + 1) % slides.length;
    slides[nextIndex].classList.add('active');
  }
}

function prevSlide(id) {
  const slider = document.getElementById(id);
  const slides = slider.querySelectorAll('.slide');
  const activeIndex = Array.from(slides).findIndex(s => s.classList.contains('active'));
  if (activeIndex !== -1) {
    slides[activeIndex].classList.remove('active');
    const prevIndex = (activeIndex - 1 + slides.length) % slides.length;
    slides[prevIndex].classList.add('active');
  }
}
</script>
<?php
include('../../inc/footer.php');
