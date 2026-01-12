/**
 * Post Collapse/Expand Feature
 * Limitiert die Höhe von langen Posts und fügt einen "Mehr anzeigen" Button hinzu
 */

// Maximale Höhe bevor Posts collapsed werden (in Pixeln)
const POST_COLLAPSE_HEIGHT = 500;

/**
 * Initialisiert die Collapse-Funktion für alle Posts
 */
function initPostCollapse() {
  const posts = document.querySelectorAll('.post');
  console.log('🔍 Post Collapse: Gefunden', posts.length, 'Posts');

  posts.forEach(post => {
    const postBody = post.querySelector('.post-body');
    const expandBtn = post.querySelector('.post-expand-btn');

    if (!postBody || !expandBtn) return;

    // Prüfe ob bereits initialisiert
    if (expandBtn.hasAttribute('data-collapse-init')) return;

    // Prüfe ob der Post zu hoch ist
    const actualHeight = postBody.scrollHeight;

    if (actualHeight > POST_COLLAPSE_HEIGHT) {
      // Post ist zu lang → Collapse aktivieren
      postBody.classList.add('collapsed');
      expandBtn.style.display = 'block';
      expandBtn.setAttribute('data-collapse-init', 'true');
      console.log('📏 Post zu lang:', actualHeight, 'px → Collapse aktiviert');
    } else {
      // Post ist kurz genug → Button versteckt lassen
      expandBtn.style.display = 'none';
      expandBtn.setAttribute('data-collapse-init', 'true');
    }
  });
}

/**
 * Toggle zwischen collapsed und expanded State
 */
function togglePost(postBody, button) {
  const isCollapsed = postBody.classList.contains('collapsed');
  const textSpan = button.querySelector('.expand-text');

  if (isCollapsed) {
    // Expand
    postBody.classList.remove('collapsed');
    textSpan.textContent = 'Weniger anzeigen';
  } else {
    // Collapse
    postBody.classList.add('collapsed');
    textSpan.textContent = 'Post vollständig anzeigen';

    // Smooth scroll zum Anfang des Posts
    postBody.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  }
}

/**
 * Re-initialisiere nach dynamischem Laden von Posts
 * (z.B. nach Infinite Scroll oder Feed-Reload)
 */
function reinitPostCollapse() {
  // Entferne alte Event Listener durch Klonen (verhindert doppelte Listener)
  const posts = document.querySelectorAll('.post');

  posts.forEach(post => {
    const expandBtn = post.querySelector('.post-expand-btn');
    if (!expandBtn) return;

    // Klone Button um alte Listener zu entfernen
    const newBtn = expandBtn.cloneNode(true);
    expandBtn.parentNode.replaceChild(newBtn, expandBtn);
  });

  // Neu initialisieren
  initPostCollapse();
}

// Auto-Init bei Page Load
document.addEventListener('DOMContentLoaded', () => {
  initPostCollapse();

  // Event Delegation: Fange alle Klicks auf expand-Buttons ab
  // Funktioniert auch für nachträglich geladene Posts!
  document.addEventListener('click', (e) => {
    const expandBtn = e.target.closest('.post-expand-btn');
    if (!expandBtn) return;

    e.preventDefault();

    const post = expandBtn.closest('.post');
    if (!post) return;

    const postBody = post.querySelector('.post-body');
    if (!postBody) return;

    togglePost(postBody, expandBtn);
  });
});

// Falls Posts dynamisch geladen werden, exportiere die Funktion
window.initPostCollapse = initPostCollapse;
window.reinitPostCollapse = reinitPostCollapse;
