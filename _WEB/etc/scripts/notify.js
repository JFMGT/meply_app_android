function notify(message, customClass = '') {
  const box = document.getElementById('notification');

  // Basis + Inhalt
  box.className = 'notification';
  box.textContent = message;

  // Klasse(n) setzen
  if (customClass) box.classList.add(customClass);
  box.classList.add('visible');

  // Nach 4 Sekunden → .visible entfernen (z. B. Slide-Out)
  setTimeout(() => {
    box.classList.remove('visible');

    // Dann z. B. 300ms später → Stilklasse entfernen
    if (customClass) {
      setTimeout(() => {
        box.classList.remove(customClass);
      }, 300); // Dauer deines Slide-Outs anpassen
    }
  }, 4000);
}

// URL-Parameter auslesen und ggf. notify aufrufen
window.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  const notificationMessage = params.get('notification');

  if (notificationMessage) {
    notify(decodeURIComponent(notificationMessage));
  }
});

