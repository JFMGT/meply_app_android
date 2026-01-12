<?php include('inc/header.php'); ?>
 
<div class="content top-margin content-map">
  
                <?php renderMapConsent(); ?>
    <div class="container">
        <div id="list-container" class="left">
            <div id="cafe-list">
            </div>
        </div>
        <div id="map-container" class="right">
            

<div id="map" style="height: 100vh; display: none;"></div>
<script src="<?= WEBSITE_BASE ?>etc/scripts/leaflet.js"></script>
<script src="<?= WEBSITE_BASE ?>etc/scripts/leaflet-markercluster.js"></script>
<script src="<?= WEBSITE_BASE ?>etc/scripts/map-consent.js"></script>
       <!--    <script type="text/javascript" src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js" id="leaflet-script-js"></script>
<script type="text/javascript" src="https://unpkg.com/leaflet.markercluster@1.4.1/dist/leaflet.markercluster.js" id="leaflet-cluster-js"></script>-->

<script>
  document.addEventListener('DOMContentLoaded', function () {
    const mapContainer = document.getElementById('map');
    const userListContainer = document.getElementById('cafe-list');

    function initMap() {
      mapContainer.style.display = 'block';

      const map = L.map('map').setView([51.505, 10.09], 6);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(map);

      const markers = L.layerGroup();
      map.addLayer(markers);

      function fetchAndRenderUsers() {
        const bounds = map.getBounds();
        const sw = bounds.getSouthWest();
        const ne = bounds.getNorthEast();
        const url = `/inc/api/get_users.php?swLat=${sw.lat}&swLng=${sw.lng}&neLat=${ne.lat}&neLng=${ne.lng}`;

        fetch(url)
          .then(async res => {
            const text = await res.text();
            let data;
            try {
              data = JSON.parse(text);
            } catch (e) {
              console.error("Fehler beim Parsen von JSON:", e);
              return;
            }

            if (!Array.isArray(data)) {
              console.warn("Erwartetes Array nicht erhalten. Rückgabe:", data);
              return;
            }

            const users = data;
            const coordMap = {};
            users.forEach(user => {
              const lat = parseFloat(user.lat);
              const lng = parseFloat(user.lng);
              if (isNaN(lat) || isNaN(lng)) return;
              const key = `${lat.toFixed(6)},${lng.toFixed(6)}`;
              if (!coordMap[key]) coordMap[key] = [];
              coordMap[key].push(user);
            });

            markers.clearLayers();
            userListContainer.innerHTML = '';

            Object.entries(coordMap).forEach(([key, group]) => {
              const [lat, lng] = key.split(',').map(parseFloat);
              if (isNaN(lat) || isNaN(lng)) return;

              const popupContent = group.length === 1
                ? `<strong>${group[0].username}</strong>`
                : `<strong>${group.length} Benutzer in dieser Region</strong>`;
              const marker = L.marker([lat, lng]).bindPopup(popupContent);
              markers.addLayer(marker);
            });

            users.sort((a, b) => b.score - a.score);
            users.slice(0, 100).forEach(user => {
              const userItem = document.createElement('div');
              userItem.className = 'cafe-item';
              userItem.setAttribute('data-id', user.id);
              userItem.innerHTML = `
                <h3>${user.username}</h3>
                <div class="user-details">
                    <p>${user.postalCode} ${user.city}</p>
                    ${user.matchScore !== null && user.matchScore !== undefined ? `<p>🎯 Matching-Score: ${(user.matchScore * 100).toFixed(0)}%</p>` : ''}
                    ${user.sharedCount !== null && user.sharedCount !== undefined ? `<p>🧩 Gemeinsame Bewertungen: ${user.sharedCount}</p>` : ''}
                    ${user.distance !== null && user.distance !== undefined ? `<p>📍 Entfernung: ${user.distance} km</p>` : ''}
                    ${(user.matchScore == null && user.sharedCount == null && user.distance == null) ? `<p>ℹ️ Noch nicht genug Daten für einen Vergleich.</p>` : ''}
                    <p class="cta">
                        <a class="compact" href="${user.link}">
                            <i class="fa-solid fa-circle-info"></i>
                            <span> Details & Mitspieler</span>
                        </a>
                    </p>
                </div>
              `;
              userListContainer.appendChild(userItem);
            });
          })
          .catch(err => {
            console.error("Fehler beim Laden der Nutzer:", err);
          });
      }

      fetchAndRenderUsers();
      map.on('moveend', fetchAndRenderUsers);
    }

    // Initialisiere Map-Consent-System
    initMapConsent(initMap);
  });
</script>


        </div>
    </div>
</div>

<?php include('inc/footer.php'); ?>
