<?php
include('inc/header.php');
?>
<script src="<?= WEBSITE_BASE ?>etc/scripts/leaflet.js"></script>
<script src="<?= WEBSITE_BASE ?>etc/scripts/leaflet-markercluster.js"></script>
<script src="<?= WEBSITE_BASE ?>etc/scripts/map-consent.js"></script>
<!-- <script type="text/javascript" src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js" id="leaflet-script-js"></script>
<script type="text/javascript" src="https://unpkg.com/leaflet.markercluster@1.4.1/dist/leaflet.markercluster.js" id="leaflet-cluster-js"></script>-->

       
   
<div class="content top-margin content-map">
	 
        <?php renderMapConsent(); ?>


	<div class="container">

		<div id="list-container" class="left">


        <div id="cafe-list">
            <!--
            <div class="cant-load">
                <h1 style="color:white">Keine Cookies :(</h1>
            <div class="card">
                
            <span style="color:white">Huch, es scheint als könnte die Karte nicht geladen werden. Zur Darstellung der Karte müssen Cookies zugelassen werden. <br><br><a style="color:white" href="#" class="cookie-settings-link" >Hier können Sie Ihre Cookie-Einstellungen anpassen.</a><br><br><a style="color:white" href="https://meeplemates.de/cafes/?rl=1">Anschließend sollte die Seite neu geladen werden.</a></span>
                </div>
</div>
-->




            <!-- Hier werden die Kacheln dynamisch geladen -->
        </div>
    </div>
    <div id="map-container" class="right">
    <!-- Hinweistext und Zustimmungsbutton -->
   
    <div id="map" style="height: 100vh; display: none;"></div> <!-- Karte, zunächst unsichtbar -->

    <?php
    $category = isset($_GET['category']) ? strtolower($_GET['category']) : null;

    if($category == 'geschaeft'){
      $category = 'geschäft';
    }
    ?>

    <script>
      const selectedCategory = <?= json_encode($category) ?>;

      document.addEventListener('DOMContentLoaded', function () {
        const mapContainer = document.getElementById('map');

        // Funktion, um die Karte zu laden
        function loadMap() {
          // Zeige die Karte
          mapContainer.style.display = 'block';

          var map = L.map('map').setView([51.505, 10.09], 6);

          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          }).addTo(map);

          var markers = L.markerClusterGroup();
          var loadedCafes = {};
          var cafeItemsContainer = document.getElementById('cafe-list');

          function loadCafesFromStrapi() {
              let url = '/inc/api/map-proxy.php?pagination[pageSize]=100&populate[meetings][count]=true';

if (selectedCategory) {
  const categoryParam = encodeURIComponent(selectedCategory.charAt(0).toUpperCase() + selectedCategory.slice(1));
  url += `&filters[Typ][$eq]=${categoryParam}`;
}
            if (selectedCategory) {
              const categoryParam = encodeURIComponent(selectedCategory.charAt(0).toUpperCase() + selectedCategory.slice(1));
              url += `&filters[Typ][$eq]=${categoryParam}`;
            }

            fetch(url)
              .then(response => response.json())
              .then(json => {
                const data = json.data;
                markers.clearLayers();
                const newItems = [];

                data.forEach(entry => {
                  const id = entry.id;
                  const lat = entry.coordinates?.lat;
                  const lng = entry.coordinates?.lng;
                  var linkPart = entry.slug ? entry.slug : entry.documentId;

                  if (lat && lng) {
                    const marker = L.marker([lat, lng]).bindPopup(
                      `<b>${entry.Titel} | ${entry.Ort}</b><br>${entry.Beschreibung || ''}<br>
                      <a href="<?= $baseurl ?>/map/<?= $category; ?>/${linkPart}" >Info & Mitspieler</a>`
                    );

                    marker.on('click', function () {
                      highlightCafe(id);
                    });

                    markers.addLayer(marker);
                  }

                  if (!loadedCafes[id]) {
                    loadedCafes[id] = true;

                    const cafeItem = document.createElement('div');
                    cafeItem.className = 'cafe-item';
                    cafeItem.setAttribute('data-id', id);
                    cafeItem.innerHTML = `
                      <h3>${entry.Titel}</h3>
                      <div class="cafe-details">
                      <p class="cta"><a class="compact" href="<?= $baseurl ?>/map/<?= $category; ?>/${linkPart}"><i class="fa-solid fa-circle-info"></i><span> Details & Mitspieler</span></a></p>
                        ${entry.Beschreibung ? `<p>${entry.Beschreibung}</p>` : ''}
                        ${entry.Strasse || entry.Hausnummer ? `<p>${entry.Strasse || ''} ${entry.Hausnummer || ''}</p>` : ''}
                        ${entry.PLZ || entry.Ort ? `<p>${entry.PLZ || ''} ${entry.Ort || ''}</p>` : ''}
                        ${entry.Mail ? `<p><a href="mailto:${entry.Mail}">${entry.Mail}</a></p>` : ''}
                        ${entry.Telefon ? `<p><a href="tel:${entry.Telefon}">${entry.Telefon}</a></p>` : ''}
                        ${entry.Website ? `<p><a href="${entry.Website}" target="_blank">Website</a></p>` : ''}
                        ${entry.Oeffnungszeiten ? `<p><strong>Öffnungszeiten:</strong><br>${entry.Oeffnungszeiten.replace(/\n/g, '<br>')}</p>` : ''}
                      </div>
                    `;

                    cafeItem.addEventListener('click', function () {
                      if (lat && lng) {
                        map.flyTo([lat, lng], 14, { duration: 1.5 });
                        highlightCafe(id);
                      }
                    });

                    newItems.push(cafeItem);
                  }
                });

                map.addLayer(markers);
                map.invalidateSize();

                newItems.sort((a, b) => {
                  var nameA = a.querySelector('h3').innerText.toUpperCase();
                  var nameB = b.querySelector('h3').innerText.toUpperCase();
                  return nameA.localeCompare(nameB);
                });

                newItems.forEach(item => {
                  cafeItemsContainer.appendChild(item);
                });
              });
          }

          function highlightCafe(cafeId) {
            var cafeItems = document.querySelectorAll('.cafe-item');
            cafeItems.forEach(item => item.classList.remove('highlighted'));

            var selectedItem = document.querySelector('.cafe-item[data-id="' + cafeId + '"]');
            if (selectedItem) {
              selectedItem.classList.add('highlighted');

              var headerOffset = 100;
              var elementPosition = selectedItem.getBoundingClientRect().top;
              var offsetPosition = elementPosition + window.pageYOffset - headerOffset;

              window.scrollTo({
                top: offsetPosition -50,
                behavior: 'smooth'
              });
            }
          }

          loadCafesFromStrapi();
          map.on('moveend zoomend', function () {
            loadCafesFromStrapi();
          });
        }

        // Initialisiere Map-Consent-System
        initMapConsent(loadMap);
      });
    </script>
</div>
</div>

</div>





<?php
include('inc/footer.php');
