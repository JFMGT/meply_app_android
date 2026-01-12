<?php 
// Sicherstellen, dass $baseUrl definiert ist (z.B. in config.php)
require_once('../../inc/config.php'); 
include('../../inc/header_auth.php');

// SICHERHEIT: Output Escaping für baseUrl falls nötig
$safeBaseUrl = htmlspecialchars($baseUrl ?? '', ENT_QUOTES, 'UTF-8');
?>
 
<div class="content">
    <div class="feed">
        </div>    
    
    <div class="thread-view" style="display:none;">
        <button class="back-to-feed">
            <img src="<?= $safeBaseUrl ?>/etc/images/back_arrow.png" alt="Zurück"> Post
        </button>
        <div class="thread-content"></div>
    </div>
</div>

<!-- Scroll-to-Top Button -->
<button class="scroll-to-top" id="scrollToTop">
  <i class="fa-solid fa-arrow-up"></i>
</button>

<script>
document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elemente ---
    const feedEl = document.querySelector('.feed');
    const threadEl = document.querySelector('.thread-view');
    const threadContentEl = threadEl.querySelector('.thread-content');
    const backBtn = threadEl.querySelector('.back-to-feed');

    // --- State ---
    let isLoading = false;
    let allLoaded = false;
    let currentCursor = null; // Speichert den Zeitstempel für die nächste Seite

    // Lightbox-Funktionen kommen aus lightbox.js (im footer.php geladen)

    // --- Scroll-to-Top Button ---
    const scrollToTopBtn = document.getElementById('scrollToTop');
    let scrollPosition = 0;

    // Zeige/Verstecke Button basierend auf Scroll-Position
    window.addEventListener('scroll', () => {
        scrollPosition = window.pageYOffset || document.documentElement.scrollTop;

        if (scrollPosition > 500) {
            scrollToTopBtn.classList.add('visible');
        } else {
            scrollToTopBtn.classList.remove('visible');
        }
    });

    // Smooth Scroll nach oben beim Klick
    scrollToTopBtn.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });

    // --- Skeleton Loading ---
    function showSkeletonLoading() {
        const skeletonHTML = `
            <div class="skeleton-posts">
                ${Array(3).fill('').map(() => `
                    <div class="skeleton-post">
                        <div class="skeleton skeleton-avatar"></div>
                        <div class="skeleton skeleton-line short"></div>
                        <div class="skeleton skeleton-line medium"></div>
                        <div class="skeleton skeleton-line long"></div>
                        <div class="skeleton skeleton-image"></div>
                    </div>
                `).join('')}
            </div>
        `;
        feedEl.insertAdjacentHTML('beforeend', skeletonHTML);
    }

    function removeSkeletonLoading() {
        const skeletons = feedEl.querySelectorAll('.skeleton-posts');
        skeletons.forEach(s => s.remove());
    }

    // --- Feed Loader ---
  function loadFeed(reset = false) {
    if (isLoading) return;
    if (!reset && allLoaded) return;

    isLoading = true;

    // Skeleton Loading anzeigen
    if (!reset) {
        showSkeletonLoading();
    }

    const params = new URLSearchParams();
    params.set('limit', 10);
    if (reset) {
        params.set('reset', '1');
        currentCursor = null;
        allLoaded = false;
        feedEl.innerHTML = '';
    }
    if (currentCursor) {
        params.set('before', currentCursor);
    }

    console.log('🔄 Lade Feed:', {
        reset,
        currentCursor,
        allLoaded,
        params: params.toString()
    });

    fetch('../../inc/api/feed.php?' + params.toString())
        .then(res => {
            if (!res.ok) {
                return res.json().then(data => {
                    throw new Error(data.error || `HTTP ${res.status}`);
                });
            }
            return res.json();
        })
        .then(data => {
            console.log('📦 Feed Antwort:', data);

            if (!data.success) {
                throw new Error(data.error || 'Unbekannter Fehler');
            }

            // Cursor aktualisieren
            if (data.cursor) {
                currentCursor = data.cursor;
                console.log('✅ Neuer Cursor:', currentCursor);
            } else {
                console.log('⚠️ Kein Cursor in Antwort');
            }
            
            // hasMore Status
            if (data.hasMore === false) {
                allLoaded = true;
                console.log('🏁 Alle Posts geladen');
            } else {
                console.log('➡️ Mehr Posts verfügbar');
            }

            // HTML einfügen
            if (data.html && data.html.trim().length > 0) {
                feedEl.insertAdjacentHTML('beforeend', data.html);
                console.log('✅ HTML eingefügt');

                // Post Collapse initialisieren für neu geladene Posts
                setTimeout(() => {
                    if (typeof window.initPostCollapse === 'function') {
                        window.initPostCollapse();
                    }
                }, 100);
            } else if (!reset) {
                allLoaded = true;
                console.log('⚠️ Kein HTML erhalten, setze allLoaded=true');
            }

            // Ende-Anzeige
            if (allLoaded && !feedEl.querySelector('.end-of-feed')) {
                const end = document.createElement('p');
                end.className = 'end-of-feed';
                end.textContent = '🎉 Du bist auf dem neuesten Stand!';
                end.style.textAlign = 'center';
                end.style.padding = '20px';
                end.style.position = 'relative';
                end.style.top = '-30px';
                end.style.color = '#888';
                feedEl.appendChild(end);
            }
        })
        .catch(err => {
            console.error('❌ Feed Error:', err);
            feedEl.insertAdjacentHTML('beforeend', 
                '<div class="card error" style="padding:20px;margin:10px;background:#fee;border:1px solid #c00;border-radius:8px;">❌ ' + 
                (err.message || 'Fehler beim Laden. Bitte später erneut versuchen.') + 
                '</div>'
            );
        })
        .finally(() => {
            removeSkeletonLoading();
            isLoading = false;
            console.log('✅ Loading beendet');
        });
}

    // --- Thread Viewer ---
    function showThread(documentId) {
        // Speichere aktuelle Scroll-Position vor dem Wechsel
        sessionStorage.setItem('feedScrollPosition', window.pageYOffset.toString());

        feedEl.style.display = 'none';
        threadEl.style.display = 'block';

        // Skeleton Loading für Thread
        const threadSkeleton = `
            <div class="skeleton-post">
                <div class="skeleton skeleton-avatar"></div>
                <div class="skeleton skeleton-line short"></div>
                <div class="skeleton skeleton-line medium"></div>
                <div class="skeleton skeleton-line long"></div>
                <div class="skeleton skeleton-image"></div>
            </div>
            <div class="skeleton-post">
                <div class="skeleton skeleton-avatar"></div>
                <div class="skeleton skeleton-line short"></div>
                <div class="skeleton skeleton-line long"></div>
            </div>
        `;
        threadContentEl.innerHTML = threadSkeleton;
        window.scrollTo(0, 0);

        history.pushState({ view: 'thread', id: documentId }, '', '#thread-' + documentId);

        fetch('../../inc/api/thread.php?documentId=' + encodeURIComponent(documentId))
            .then(res => {
                 if (!res.ok) throw new Error(`HTTP ${res.status}`);
                 return res.text();
            })
            .then(html => {
                threadContentEl.innerHTML = html;

                // Post Collapse auch für Thread-View initialisieren
                setTimeout(() => {
                    if (typeof window.initPostCollapse === 'function') {
                        window.initPostCollapse();
                    }
                }, 100);
            })
            .catch(err => {
                threadContentEl.innerHTML = '<div class="card error">❌ Fehler beim Laden des Threads.</div>';
                console.error(err);
            });
    }

    function showFeed() {
        threadEl.style.display = 'none';
        threadContentEl.innerHTML = ''; // Speicher freigeben
        feedEl.style.display = 'flex';

        // Scroll-Position wiederherstellen
        setTimeout(() => {
            const savedPosition = sessionStorage.getItem('feedScrollPosition');
            if (savedPosition) {
                window.scrollTo({
                    top: parseInt(savedPosition, 10),
                    behavior: 'smooth'
                });
            }
        }, 100); // Kurzer Delay damit DOM gerendert ist
    }

    // --- Event Listeners ---

    // Infinite Scroll
    window.addEventListener('scroll', () => {
        if (allLoaded || isLoading || threadEl.style.display === 'block') return;
        // Puffer auf 300px erhöht für flüssigeres Laden
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 500) {
            loadFeed(false);
        }
    });

    // Klicks im Feed (Delegation)
    feedEl.addEventListener('click', (e) => {
        // Thread öffnen
        const replyLink = e.target.closest('.show-replys, .reply-button');
        if (replyLink) {
            e.preventDefault();
            const postId = replyLink.dataset.documentid;
            if (postId) showThread(postId);
            return;
        }
        // Andere Klicks können hier behandelt werden
    });

    // Zurück-Button
    backBtn.addEventListener('click', () => {
    // ✅ Nur relativer Pfad, kein Origin
    history.replaceState({ view: 'feed' }, '', './');
    showFeed();
});

    // Browser-Zurück-Button
    window.addEventListener('popstate', (event) => {
        if (event.state && event.state.view === 'thread') {
            showThread(event.state.id);
        } else {
            showFeed();
        }
    });

    // Initial Load
    // Prüfen, ob wir direkt auf einem Thread landen (per URL-Hash)
    const hash = window.location.hash;
    if (hash && hash.startsWith('#thread-')) {
        const threadId = hash.replace('#thread-', '');
        loadFeed(true); // Feed im Hintergrund laden
        showThread(threadId);
    } else {
        loadFeed(true);
    }
});

// Slider Funktionen (müssen global sein, wenn onclick="..." im HTML verwendet wird)
// Besser wäre es, diese auch per Event Delegation im DOMContentLoaded zu binden.
window.nextSlide = function(id) {
    const slider = document.getElementById(id);
    if (!slider) return;
    const slides = slider.querySelectorAll('.slide');
    let activeIndex = -1;
    slides.forEach((s, i) => { if (s.classList.contains('active')) activeIndex = i; });
    
    if (activeIndex !== -1) {
        slides[activeIndex].classList.remove('active');
        const nextIndex = (activeIndex + 1) % slides.length;
        slides[nextIndex].classList.add('active');
    }
};

window.prevSlide = function(id) {
    const slider = document.getElementById(id);
    if (!slider) return;
    const slides = slider.querySelectorAll('.slide');
    let activeIndex = -1;
    slides.forEach((s, i) => { if (s.classList.contains('active')) activeIndex = i; });

    if (activeIndex !== -1) {
        slides[activeIndex].classList.remove('active');
        const prevIndex = (activeIndex - 1 + slides.length) % slides.length;
        slides[prevIndex].classList.add('active');
    }
};
</script>

<style>
/* Einfachere, sicherere Footer-Positionierung */
footer { 
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0; /* Statt width: 100% */
  /* width: calc(...) entfernen, wenn möglich, oder durch padding im Body lösen */
  z-index: 100; /* Sicherstellen, dass er oben liegt */
}

/* Padding für den Content, damit er nicht vom fixen Footer verdeckt wird */
body {
    padding-bottom: 80px; /* Mindestens so hoch wie der Footer */
}

.feed {
    /* padding-bottom nicht mehr nötig, wenn body padding hat */
    min-height: 50vh; /* Verhindert Springen bei leerem Feed */
}

.hidden { display: none !important; }
</style>

<?php include('../../inc/footer.php'); ?>