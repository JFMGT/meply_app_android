<?php
   require_once('esc.php');

/**
 * Prüft, ob der Nutzer bereits seine Zustimmung für OSM-Karten gegeben hat
 * @return bool True wenn Consent-Cookie gesetzt ist
 */
function hasMapConsent() {
    return isset($_COOKIE['osm_map_consent']) && $_COOKIE['osm_map_consent'] === '1';
}

/**
 * Rendert die OSM-Karten-Zustimmungsbox
 * @param string $buttonId ID des Lade-Buttons
 * @param string $consentId ID des Consent-Containers
 */
function renderMapConsent($buttonId = 'load-map-button', $consentId = 'map-consent') {
    // Wenn bereits Zustimmung vorhanden, Box verstecken
    $displayStyle = hasMapConsent() ? 'display:none;' : '';

    echo '
    <div id="' . esc($consentId) . '" class="container top-margin" style="' . $displayStyle . 'margin-top:100px">
        <div class="card" style="width:500px; max-width:100%">
            <p>Zur Nutzung dieser Funktion müssen Daten an OpenStreetMap übertragen werden. Mit einem Klick auf "Karte anzeigen" stimmen Sie der Übertragung von <strong>z. B. IP-Adressen</strong> und <strong>Standortdaten</strong> zu. Weitere Informationen zum Service und zur Datenverarbeitung finden Sie in der <a href="https://osmfoundation.org/wiki/Privacy_Policy" target="_blank" rel="noopener noreferrer">Datenschutzerklärung von OpenStreetMap</a>.</p>

            <div style="margin: 20px 0;">
                <label style="display: flex; align-items: center; gap: 10px; cursor: pointer;">
                    <input type="checkbox" id="remember-map-consent" style="width: 20px; height: 20px; cursor: pointer;">
                    <span>Auswahl merken (30 Tage)</span>
                </label>
            </div>

            <button class="cta-button" style="background-color:#fec50d; padding:10px 25px; cursor:pointer" id="' . esc($buttonId) . '">Karte laden</button>
        </div>
    </div>';
}
?>
