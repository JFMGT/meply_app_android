<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../../inc/functions.php'); // Nötig für esc(), requireLogin() etc.

// 1. SICHERHEIT: Login prüfen
requireLogin();

include("../../inc/header_auth.php");

// Daten aus Session für Vorauswahl (sicher fallbacken)
$prefillZip = $_SESSION['profile']['postalCode'] ?? '';
$prefillRadius = $_SESSION['profile']['searchRadius'] ?? '';
?>

<div class="content content-plain top-margin">
    <div class="card">
        <h1>Alle Spielergesuche</h1>
        <p>Eine Übersicht über alle Gesuche in deiner Nähe.</p>
        <form id="radius-form">
            <input type="text" 
                   name="zip" 
                   placeholder="PLZ" 
                   pattern="\d{5}"
                   title="Bitte gib eine 5-stellige PLZ ein"
                   value="<?= esc($prefillZip) ?>" 
                   required>
            
            <input type="number" 
                   name="distance" 
                   placeholder="Umkreis in km" 
                   min="1" 
                   max="200"
                   value="<?= esc($prefillRadius) ?>" 
                   required>
            
            <button type="submit">Suchen</button>
        </form>
    </div>
    
    <div id="results"></div>
</div>

<script>
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('radius-form');
    const container = document.getElementById('results');
    
    // Zentrale Lade-Funktion
    async function loadMeetings(zip, distance) {
        // Validierung vor dem Request
        if (!/^\d{5}$/.test(zip)) {
            container.innerHTML = '<div class="card error"><p>Bitte gib eine gültige 5-stellige PLZ ein.</p></div>';
            return;
        }
        
        const distNum = parseFloat(distance);
        if (isNaN(distNum) || distNum < 1 || distNum > 200) {
            container.innerHTML = '<div class="card error"><p>Bitte gib einen Umkreis zwischen 1 und 200 km ein.</p></div>';
            return;
        }
        
        // Loading Indicator
        container.innerHTML = '<div class="card"><p class="loading-dots">Lade Ergebnisse...</p></div>';
        
        try {
            // ✅ FIX: Korrekte Template Literal Syntax
            const res = await fetch(`../../inc/api/getMeetingsByZip.php?zip=${encodeURIComponent(zip)}&distance=${encodeURIComponent(distance)}`);
            
            if (!res.ok) {
                throw new Error(`HTTP Fehler ${res.status}`);
            }
            
            const data = await res.json();
            
            if (!data.success && data.error) {
                // Fehler vom Backend
                container.innerHTML = `<div class="card error"><p>${escapeHtml(data.error)}</p></div>`;
            } else if (data.content) {
                // Erfolg: HTML vom Backend rendern
                container.innerHTML = data.content;
                
                // Optional: Anzahl anzeigen
                if (data.count > 0) {
                    const countInfo = document.createElement('div');
                    countInfo.className = 'card info';
                    countInfo.innerHTML = `<p>${data.count} Gesuch${data.count !== 1 ? 'e' : ''} gefunden</p>`;
                    container.insertBefore(countInfo, container.firstChild);
                }
            } else {
                // Kein Content, aber auch kein expliziter Fehler
                container.innerHTML = '<div class="card"><p>Keine Gesuche gefunden.</p></div>';
            }
        } catch (err) {
            console.error('Fehler beim Abrufen:', err);
            container.innerHTML = '<div class="card error"><p>Fehler beim Laden der Daten. Bitte versuche es später erneut.</p></div>';
        }
    }
    
    // ✅ Helper: Einfaches HTML-Escaping für Fehler-Messages
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // Event Listener für Formular
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const zip = this.zip.value.trim();
        const distance = this.distance.value.trim();
        loadMeetings(zip, distance);
    });
    
    // Automatischer Start, wenn Werte vorhanden
    const initialZip = form.zip.value.trim();
    const initialDist = form.distance.value.trim();
    
    if (initialZip && initialDist) {
        loadMeetings(initialZip, initialDist);
    }
});
</script>

<?php include("../../inc/footer.php"); ?>