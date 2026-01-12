/**
 * OSM Map Consent Handler
 * Verwaltet die Nutzer-Zustimmung für das Laden von OpenStreetMap-Karten
 */

// Cookie-Helfer-Funktionen
function setCookie(name, value, days) {
  const date = new Date();
  date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
  const expires = "expires=" + date.toUTCString();
  document.cookie = name + "=" + value + ";" + expires + ";path=/;SameSite=Lax";
}

function getCookie(name) {
  const nameEQ = name + "=";
  const ca = document.cookie.split(';');
  for(let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) == ' ') c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length, c.length);
  }
  return null;
}

/**
 * Prüft, ob Consent bereits erteilt wurde
 * @returns {boolean}
 */
function hasMapConsent() {
  return getCookie('osm_map_consent') === '1';
}

/**
 * Speichert die Consent-Entscheidung
 */
function saveMapConsent() {
  setCookie('osm_map_consent', '1', 30); // 30 Tage gültig
}

/**
 * Initialisiert das Map-Consent-System
 * @param {Function} loadMapCallback - Funktion die aufgerufen wird, um die Karte zu laden
 * @param {string} buttonId - ID des Lade-Buttons
 * @param {string} consentId - ID des Consent-Containers
 */
function initMapConsent(loadMapCallback, buttonId = 'load-map-button', consentId = 'map-consent') {
  const loadMapButton = document.getElementById(buttonId);
  const mapConsent = document.getElementById(consentId);
  const rememberCheckbox = document.getElementById('remember-map-consent');

  // Wenn bereits Zustimmung erteilt wurde, Karte direkt laden
  if (hasMapConsent()) {
    if (mapConsent) mapConsent.style.display = 'none';
    if (typeof loadMapCallback === 'function') {
      loadMapCallback();
    }
    return;
  }

  // Event Listener für den Lade-Button
  if (loadMapButton) {
    loadMapButton.addEventListener('click', function() {
      // Wenn "Auswahl merken" aktiviert ist, Cookie setzen
      if (rememberCheckbox && rememberCheckbox.checked) {
        saveMapConsent();
      }

      // Consent-Box verstecken
      if (mapConsent) {
        mapConsent.style.display = 'none';
      }

      // Karte laden
      if (typeof loadMapCallback === 'function') {
        loadMapCallback();
      }
    });
  }
}

// Mache Funktionen global verfügbar
window.initMapConsent = initMapConsent;
window.hasMapConsent = hasMapConsent;
