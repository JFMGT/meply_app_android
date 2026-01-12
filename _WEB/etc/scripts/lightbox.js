/**
 * Moderne Lightbox mit Galerie, Zoom, Keyboard & Swipe Support
 */

let lightboxState = {
  images: [],
  currentIndex: 0,
  zoomLevel: 1,
  isDragging: false,
  startX: 0,
  startY: 0,
  translateX: 0,
  translateY: 0
};

/**
 * Öffnet die Lightbox mit einem oder mehreren Bildern
 * @param {string|array} urlOrArray - Einzelnes Bild-URL oder Array von Bildern
 * @param {string} alt - Alt-Text (wird ignoriert bei Array)
 * @param {number} startIndex - Start-Index bei Array
 */
function openLightbox(urlOrArray, alt = '', startIndex = 0) {
  const lightbox = document.getElementById('lightbox');
  const img = lightbox.querySelector('.lightbox-image');
  const caption = lightbox.querySelector('.lightbox-caption');
  const prevBtn = lightbox.querySelector('.lightbox-prev');
  const nextBtn = lightbox.querySelector('.lightbox-next');
  const counter = lightbox.querySelector('.lightbox-counter');
  const zoomControls = lightbox.querySelector('.lightbox-zoom-controls');
  const downloadBtn = lightbox.querySelector('.lightbox-download');

  // Reset State
  lightboxState.zoomLevel = 1;
  lightboxState.translateX = 0;
  lightboxState.translateY = 0;
  lightboxState.currentIndex = startIndex;

  // Bilder verarbeiten
  if (Array.isArray(urlOrArray)) {
    lightboxState.images = urlOrArray;
  } else {
    lightboxState.images = [{ url: urlOrArray, alt: alt }];
  }

  // Erstes Bild laden
  updateLightboxImage();

  // Navigation Buttons ein/ausblenden
  if (lightboxState.images.length > 1) {
    prevBtn.style.display = 'flex';
    nextBtn.style.display = 'flex';
    counter.style.display = 'block';
  } else {
    prevBtn.style.display = 'none';
    nextBtn.style.display = 'none';
    counter.style.display = 'none';
  }

  // Zoom-Controls und Download-Button anzeigen
  if (zoomControls) zoomControls.style.display = 'flex';
  if (downloadBtn) downloadBtn.style.display = 'flex';

  lightbox.classList.remove('hidden');
  lightbox.classList.add('visible');
  document.body.style.overflow = 'hidden'; // Verhindere Scrollen im Hintergrund
}

/**
 * Aktualisiert das angezeigte Bild in der Lightbox
 */
function updateLightboxImage() {
  const lightbox = document.getElementById('lightbox');
  const img = lightbox.querySelector('.lightbox-image');
  const caption = lightbox.querySelector('.lightbox-caption');
  const counter = lightbox.querySelector('.lightbox-counter');

  const current = lightboxState.images[lightboxState.currentIndex];

  img.src = current.url;
  img.alt = current.alt || 'Bild';
  caption.textContent = current.alt || '';

  // Counter aktualisieren
  if (lightboxState.images.length > 1) {
    counter.textContent = `${lightboxState.currentIndex + 1} / ${lightboxState.images.length}`;
  }

  // Reset Zoom & Position
  lightboxState.zoomLevel = 1;
  lightboxState.translateX = 0;
  lightboxState.translateY = 0;
  applyTransform();
}

/**
 * Navigation: Vorheriges Bild
 */
function lightboxPrev() {
  if (lightboxState.currentIndex > 0) {
    lightboxState.currentIndex--;
    updateLightboxImage();
  }
}

/**
 * Navigation: Nächstes Bild
 */
function lightboxNext() {
  if (lightboxState.currentIndex < lightboxState.images.length - 1) {
    lightboxState.currentIndex++;
    updateLightboxImage();
  }
}

/**
 * Schließt die Lightbox
 */
function closeLightbox() {
  const lightbox = document.getElementById('lightbox');
  lightbox.classList.remove('visible');
  lightbox.classList.add('hidden');
  document.body.style.overflow = ''; // Scrollen wieder erlauben

  // Reset State
  lightboxState.images = [];
  lightboxState.currentIndex = 0;
  lightboxState.zoomLevel = 1;
  lightboxState.translateX = 0;
  lightboxState.translateY = 0;
}

/**
 * Zoom-Funktion
 */
function applyTransform() {
  const img = document.querySelector('.lightbox-image');
  img.style.transform = `scale(${lightboxState.zoomLevel}) translate(${lightboxState.translateX}px, ${lightboxState.translateY}px)`;
}

/**
 * Zoom In/Out mit Scroll
 */
function handleZoom(e) {
  if (!document.getElementById('lightbox').classList.contains('visible')) return;

  e.preventDefault();

  const delta = e.deltaY > 0 ? -0.1 : 0.1;
  lightboxState.zoomLevel = Math.min(Math.max(1, lightboxState.zoomLevel + delta), 3);

  // Reset Position wenn Zoom = 1
  if (lightboxState.zoomLevel === 1) {
    lightboxState.translateX = 0;
    lightboxState.translateY = 0;
  }

  applyTransform();
}

/**
 * Zoom In
 */
function zoomIn() {
  lightboxState.zoomLevel = Math.min(3, lightboxState.zoomLevel + 0.3);
  applyTransform();
}

/**
 * Zoom Out
 */
function zoomOut() {
  lightboxState.zoomLevel = Math.max(1, lightboxState.zoomLevel - 0.3);
  if (lightboxState.zoomLevel === 1) {
    lightboxState.translateX = 0;
    lightboxState.translateY = 0;
  }
  applyTransform();
}

/**
 * Reset Zoom
 */
function resetZoom() {
  lightboxState.zoomLevel = 1;
  lightboxState.translateX = 0;
  lightboxState.translateY = 0;
  applyTransform();
}

/**
 * Download aktuelles Bild
 */
function downloadImage() {
  const current = lightboxState.images[lightboxState.currentIndex];
  const link = document.createElement('a');
  link.href = current.url;
  link.download = `meply-image-${lightboxState.currentIndex + 1}.jpg`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

/**
 * Touch/Mouse Drag für gezoomte Bilder
 */
let touchStartX = 0;
let touchStartY = 0;

function handleTouchStart(e) {
  const lightbox = document.getElementById('lightbox');
  if (!lightbox.classList.contains('visible')) return;

  if (e.touches && e.touches.length === 1) {
    touchStartX = e.touches[0].clientX;
    touchStartY = e.touches[0].clientY;
    lightboxState.isDragging = true;
  }
}

function handleTouchMove(e) {
  if (!lightboxState.isDragging) return;
  if (lightboxState.zoomLevel === 1) return; // Nur bei Zoom > 1 verschieben

  e.preventDefault();

  if (e.touches && e.touches.length === 1) {
    const deltaX = e.touches[0].clientX - touchStartX;
    const deltaY = e.touches[0].clientY - touchStartY;

    lightboxState.translateX += deltaX;
    lightboxState.translateY += deltaY;

    touchStartX = e.touches[0].clientX;
    touchStartY = e.touches[0].clientY;

    applyTransform();
  }
}

function handleTouchEnd() {
  lightboxState.isDragging = false;
}

/**
 * Swipe-Gesten für Navigation (nur bei Zoom = 1)
 */
let swipeStartX = 0;
let swipeStartTime = 0;

function handleSwipeStart(e) {
  const lightbox = document.getElementById('lightbox');
  if (!lightbox.classList.contains('visible')) return;
  if (lightboxState.zoomLevel > 1) return; // Kein Swipe bei Zoom

  if (e.touches && e.touches.length === 1) {
    swipeStartX = e.touches[0].clientX;
    swipeStartTime = Date.now();
  }
}

function handleSwipeEnd(e) {
  const lightbox = document.getElementById('lightbox');
  if (!lightbox.classList.contains('visible')) return;
  if (lightboxState.zoomLevel > 1) return;

  if (e.changedTouches && e.changedTouches.length === 1) {
    const swipeEndX = e.changedTouches[0].clientX;
    const swipeTime = Date.now() - swipeStartTime;
    const swipeDistance = swipeEndX - swipeStartX;

    // Swipe muss schnell genug sein (< 300ms) und weit genug (> 50px)
    if (swipeTime < 300 && Math.abs(swipeDistance) > 50) {
      if (swipeDistance > 0) {
        // Swipe nach rechts → Vorheriges Bild
        lightboxPrev();
      } else {
        // Swipe nach links → Nächstes Bild
        lightboxNext();
      }
    }
  }
}

/**
 * Keyboard Shortcuts
 */
document.addEventListener('keydown', (e) => {
  const lightbox = document.getElementById('lightbox');
  if (!lightbox.classList.contains('visible')) return;

  switch(e.key) {
    case 'Escape':
      closeLightbox();
      break;
    case 'ArrowLeft':
      e.preventDefault();
      lightboxPrev();
      break;
    case 'ArrowRight':
      e.preventDefault();
      lightboxNext();
      break;
    case '+':
    case '=':
      e.preventDefault();
      lightboxState.zoomLevel = Math.min(3, lightboxState.zoomLevel + 0.2);
      applyTransform();
      break;
    case '-':
      e.preventDefault();
      lightboxState.zoomLevel = Math.max(1, lightboxState.zoomLevel - 0.2);
      if (lightboxState.zoomLevel === 1) {
        lightboxState.translateX = 0;
        lightboxState.translateY = 0;
      }
      applyTransform();
      break;
  }
});

// Event Listeners initialisieren
document.addEventListener('DOMContentLoaded', () => {
  const lightbox = document.getElementById('lightbox');
  if (!lightbox) return;

  const img = lightbox.querySelector('.lightbox-image');

  // Zoom mit Scroll
  lightbox.addEventListener('wheel', handleZoom, { passive: false });

  // Touch Events für Drag & Swipe
  lightbox.addEventListener('touchstart', (e) => {
    handleTouchStart(e);
    handleSwipeStart(e);
  });

  lightbox.addEventListener('touchmove', handleTouchMove, { passive: false });

  lightbox.addEventListener('touchend', (e) => {
    handleTouchEnd();
    handleSwipeEnd(e);
  });

  // Mouse Drag für Desktop
  img.addEventListener('mousedown', (e) => {
    if (lightboxState.zoomLevel === 1) return;
    e.preventDefault();
    lightboxState.isDragging = true;
    touchStartX = e.clientX;
    touchStartY = e.clientY;
  });

  document.addEventListener('mousemove', (e) => {
    if (!lightboxState.isDragging) return;

    const deltaX = e.clientX - touchStartX;
    const deltaY = e.clientY - touchStartY;

    lightboxState.translateX += deltaX;
    lightboxState.translateY += deltaY;

    touchStartX = e.clientX;
    touchStartY = e.clientY;

    applyTransform();
  });

  document.addEventListener('mouseup', () => {
    lightboxState.isDragging = false;
  });
});

// Mache Funktionen global verfügbar
window.openLightbox = openLightbox;
window.closeLightbox = closeLightbox;
window.lightboxPrev = lightboxPrev;
window.lightboxNext = lightboxNext;
window.zoomIn = zoomIn;
window.zoomOut = zoomOut;
window.resetZoom = resetZoom;
window.downloadImage = downloadImage;
