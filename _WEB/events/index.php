<?php include('inc/header.php');
     
 ?>
   <div class="content content-plain top-margin">
    <!-- wp:heading -->
<h2 class="wp-block-heading">Entdecke die besten Brettspiel-Events in deiner Nähe und deutschlandweit!</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Willkommen auf unserer Event-Übersicht! Hier findest du eine Vielzahl an spannenden Veranstaltungen für Brettspiel-Enthusiasten – von Spieleabenden in gemütlichen Cafés bis hin zu großen Messen und Turnieren. Egal, ob du auf der Suche nach neuen Spielen bist, Mitspieler treffen möchtest oder an einem Turnier teilnehmen willst – bei uns wirst du fündig.</p>
<!-- /wp:paragraph --></div>

<div class="content content-plain">
    <form id="event-filter">
        <div class="form-40">
        <label for="zip">Ihre PLZ:</label>
        <?php 

        $userZip = $_SESSION['profile']['postalCode'];
        $userRadius = $_SESSION['profile']['searchRadius'];
        ?>
        <input value="<?= $userZip ?>" type="text" id="zip" name="zip" placeholder="PLZ eingeben">
        </div>

        <div class="form-40">
        <label for="radius">Entfernung (km):</label>
        <input value="<?= $userRadius ?>" type="number" id="radius" name="radius" placeholder="Radius in km" min="0">
        </div>
        <div class="form-20">
        <button type="submit">Filter anwenden</button>
    </div>
    </form>
    </div>
    <div class="content">
    <div class="events">
        <!-- Hier werden die Events dynamisch via AJAX geladen -->
    </div>
    <div class="search_note"><i class="fa-solid fa-circle-info"></i> Aktuell siehst du nur Veranstaltungen im Umkreis von <span></span> km. Möchtest du weiter schauen? Passe den Filter oben an, um Veranstaltungen in einem größeren Radius zu entdecken!<br> Deinen bevorzugten Suchradius kannst du auch dauerhaft in deinem Profil anpassen.</div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('event-filter');
    const zipInput = document.getElementById('zip');
    const radiusInput = document.getElementById('radius');
    const eventsContainer = document.querySelector('.events');
    const searchNote = document.querySelector('.search_note span');

    function loadEvents(zip = '', radius = 0) {
        fetch('/inc/api/events.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `zip=${encodeURIComponent(zip)}&radius=${encodeURIComponent(radius)}`
        })
        .then(response => response.json())
        .then(data => {
            console.log(data);
            
            // WICHTIG: Prüfe zuerst auf Fehler
            if (data.error) {
                eventsContainer.innerHTML = `<p style="color:white">${data.error}</p>`;
                document.querySelector('.search_note').classList.remove('active');
                return;
            }
            
            // Kein Fehler: zeige HTML oder Fallback
            eventsContainer.innerHTML = data.html || '<p style="color:white">Keine Events gefunden.</p>';
            
            if (!isNaN(zip) && zip !== '' && !isNaN(radius) && radius > 0) {
                document.querySelector('.search_note').classList.add('active');
                searchNote.textContent = radius;
            } else {
                document.querySelector('.search_note').classList.remove('active');
            }
        })
        .catch(error => {
            console.error('Fetch-Fehler:', error);
            eventsContainer.innerHTML = '<p style="color:white">Ein Fehler ist aufgetreten. Bitte versuche es später erneut.</p>';
        });
    }

    // Direkt laden
    loadEvents(zipInput.value, radiusInput.value);

    // Beim Absenden des Filters
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const zip = zipInput.value;
        const radius = radiusInput.value;
        console.log(radius);
        loadEvents(zip, radius);
    });
});

</script>
<?php
include('inc/footer.php');