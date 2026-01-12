<?php
include("../inc/header_auth.php");
$jwt = $_SESSION['jwt'];
$profileId = $_SESSION['profile']['documentId'];
?>

<style>
  .dropdown {
    border: 1px solid #ccc;
    max-height: 200px;
    overflow-y: auto;
    position: absolute;
    background: #151d26;
    width: 300px;
  }

  .filters{
    display:flex;
    flex-wrap: wrap;
    gap: 15px;
    width:100%;
  }

  .filters label{
    margin-right:25px;
    width:25%;
    width:calc(25% - 40px);
  }

  .filters label select, .filters label input{
    width:100%;
  }

  /* Sortier-Buttons */
  .sort-buttons {
    display: flex;
    gap: 10px;
    margin: 15px 0;
  }

  .sort-buttons button {
    padding: 8px 16px;
    border: 2px solid #444;
    background: #1c242d;
    color: #f0f0f0;
    border-radius: 5px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .sort-buttons button:hover {
    background: #2a3542;
  }

  .sort-buttons button.active {
    background: #fec50d;
    color: #000;
    border-color: #fec50d;
    font-weight: bold;
  }

  .dropdown div {
    padding: 6px;
    cursor: pointer;
  }
  .dropdown div:hover {
    background: #eee;
  }
  .modal {
    display: none;
    position: fixed;
    top: 20%;
    width: 30%;
    left: calc(50% - 15%);
    background: #2f3444;
    padding: 20px;
    border: 1px solid #aaa;
    right: auto;
    height: 600px;
  }

  #saleModal {
  display: none;
  position: fixed;
  top: 15%;
  left: calc(50% - 15%);
  width: 30%;
  background: #2f3444;
  color: #f0f0f0;
  border: 1px solid #444;
  padding: 20px;
  border-radius: 6px;
  z-index: 1000;
}

#saleModal h3 {
  margin-top: 0;
  color: #fec50d;
}

#saleModal label {
  display: block;
  margin-bottom: 10px;
}


  ul#gameList {
    list-style: none;
    padding: 0;
    margin-top: 1rem;
  }

  ul#gameList li {
    background: #1c242d;
    color: #f0f0f0;
    padding: 8px 12px;
    margin-bottom: 6px;
    border-radius: 6px;
    border: 1px solid #2a3542;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.9rem;
  }

  ul#gameList li i{
    cursor:pointer;
  }

  .game-left {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .game-controls {
    display: flex;
    align-items: center;
    gap: 8px;
  }



  .stars {
    display: inline-block;
    direction: ltr;
    unicode-bidi: bidi-override;
    font-size: 1.3rem;
  }

  .star {
    position: relative;
    display: inline-block;
    color: #444;
    cursor: pointer;
    width: 1.2em;
  }

  .star::before {
    content: "☆";
    position: relative;
    z-index: 1;
  }

  .star.full::before {
    content: "★";
    color: gold;
  }

  .star.half::before {
    content: "★";
    color: gold;
    position: absolute;
    width: 50%;
    overflow: hidden;
    z-index: 2;
  }

  .remove-btn {
    background: none;
    border: none;
    color: #999;
    cursor: pointer;
    font-size: 1.1rem;
    margin-left: 10px;
  }

  .star {

  position: relative;
  display: inline-block;
  color: #444;
  cursor: pointer;
  width: 1.2em;
  height: 1.2em;
}

.add-games{
  display:flex;
  justify-content:space-between;
}

.cta-upload{
  background-color: #fec50d;
  color:black;
  height:25px;
  border-radius: 5px;
  padding:10px;
     
    text-decoration: none;
}

  .remove-btn:hover {
    color: #e66;
  }

  ul#gameList li {
  background: #1c242d;
  color: #f0f0f0;
  padding: 8px 12px 8px 36px;
  margin-bottom: 6px;
  border-radius: 6px;
  border: 1px solid #2a3542;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  position: relative;
}

.sale-icon {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1rem;
  color: #888;
  background: transparent;
  border-radius: 4px;
  padding: 2px 4px;
  transition: all 0.2s ease;
}

.sale-icon.active {
  color: #1c242d;
  background: #facc15;
  box-shadow: 0 0 4px #facc15aa;
}

.modal-overlay {
  display: none;
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 999;
  justify-content: center;
  align-items: center;
}

.modal-window {
  background: #2f3444;
  color: #f0f0f0;
  border: 1px solid #444;
  border-radius: 8px;
  padding: 20px;
  width: 90%;
  max-width: 500px;
  max-height: 70vh;
  overflow-y: auto;
  box-shadow: 0 0 25px rgba(0,0,0,0.5);
  animation: popin 0.2s ease-out;
}

@keyframes popin {
  from { transform: scale(0.95); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}

.modal-window h3 {
  margin-top: 0;
  color: #fec50d;
}
.modal-window button {
  margin-top: 8px;
}

@media (max-width: 768px) {
  ul#gameList li {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px 10px 12px 36px;
    position: relative;
  }

  .game-left {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 8px;
    margin-bottom: 8px;
  }

  .game-left .game-title {
    font-weight: bold;
    flex: 1;
  }

  .game-controls {
    width: 100%;
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
  }

  .stars {
    order: 1;
    flex-grow: 1;
  }

  .status-select {
    order: 2;
    flex: 0 0 48%;
  }

  .remove-btn {
    position: absolute;
    right: 10px;
    top: 25px;
    transform: translateY(-50%);
  }

  .sale-icon {
    left: 10px;
    top: 16px;
    transform: none;
  }
}

</style>

<div class="content content-plain top-margin">
  <h2>Lieblingsspiele bearbeiten</h2>

  <p>Nutze dieses Feld um Spiele zu deiner Liste hinzuzufügen. Tippe die ersten Buchstaben und das System schlägt dir Spiele vor.<br>
  Sollte das Spiel nicht in der Datenbank gefunden werden, kannst du es durch betätigen der Entertaste hinzufügen. </p>
  <div class="add-games">
  <input type="text" id="gameSearch" placeholder="Spiel hinzufügen..." autocomplete="off"> 
  <a id="" class="cta-upload" href="upload.php"><i class="fa-solid fa-file-import"></i> Import</a>
  </div>
  <div id="suggestions" class="dropdown" style="display:none;"></div>
  <p id="noResults" style="color: gray;"></p>
  <br><br>
  
  <h3>Deine Lieblingsspiele:</h3>
  
  <!-- Sortier-Buttons -->
  <div class="sort-buttons">
    <button id="sortTitle" class="active">📝 A-Z</button>
    <button id="sortRating">⭐ Bewertung</button>
  </div>
  
  <p>Hier kannst du deine Liste durchsuchen und Filtern</p>
  <div class="filters">
      <label>
    Titel:
    <input type="text" id="filterTitle" placeholder="Titel suchen..." />
  </label>
  <label>
    Status:
    <select id="filterState">
      <option value="">Alle</option>
      <option value="wishlist">Will spielen</option>
      <option value="played">Habe gespielt</option>
      <option value="owned">Besitze</option>
    </select>
  </label>
  <label>
    Mind. Bewertung:
    <select id="filterRating">
      <option value="0">Alle</option>
      <option value="1">★ 1+</option>
      <option value="2">★ 2+</option>
      <option value="3">★ 3+</option>
      <option value="4">★ 4+</option>
      <option value="5">★ 5</option>
    </select>
  </label>
  <label>Trödelmarkt
    <select id="filterSale">
    <option value="0">Alle</option>
    <option value="1">Nicht Angeboten</option>
    <option value="2">Angeboten</option>
    </select>
  </label>
</div>
  
<div id="pagination_top" class="pagination-controls"></div>
  <ul id="gameList"></ul>
<div id="pagination_bottom" class="pagination-controls"></div>


  <div id="modal" class="modal">
    <h3>Neues Spiel eintragen</h3>
    <form id="newGameForm">
      <label>Titel: <input type="text" id="newTitle" required></label><br>
      <label>Min. Spieler: <input type="number" id="minPlayer"></label><br>
      <label>Max. Spieler: <input type="number" id="maxPlayer"></label><br>
      <label>Alter: <input type="number" id="minAge"></label><br>
      <label>Erscheinungsdatum: <input type="date" id="releaseDate"></label><br>
      <button type="submit">Speichern & hinzufügen</button>
      <button type="button" onclick="closeModal()">Abbrechen</button>
    </form>
  </div>
</div>

<div id="saleModalOverlay" class="modal-overlay">
  <div class="modal-window">
    <h3 id="saleModalTitle"></h3>
    <form id="saleForm">
      <label>
        Verkaufsstatus:
        <select id="saleStatus">
          <option value="false">Nicht zum Verkauf</option>
          <option value="true">Zum Verkauf</option>
        </select>
      </label><br>

      <label>Preis (€):
        <input type="number" id="salePrice" min="0" step="0.01" />
      </label><br>

      <label>Zustand:
        <select id="saleCondition">
          <option value="New">Neu</option>
          <option value="Like New">Wie neu</option>
          <option value="Very Good">Sehr gut</option>
          <option value="Good">Gut</option>
          <option value="Used">Gebraucht</option>
        </select>
      </label><br>

      <label>Beschreibung:<br>
        <textarea id="saleDescription" rows="3" style="width:90%;"></textarea>
      </label><br>

      <label>Lieferung:
        <select id="saleDelivery">
          <option value="ShippingOnly">Nur Versand</option>
          <option value="PickupOnly">Nur Abholung</option>
          <option value="ShippingOrPickup">Versand oder Abholung</option>
        </select>
      </label><br>

      <label>
        <input type="checkbox" id="saleTradePossible" /> Tausch möglich
      </label><br><br>

      <div style="display:flex; justify-content: space-between;">
        <button type="submit">Speichern</button>
        <button type="button" onclick="closeSaleModal()">Abbrechen</button>
      </div>
    </form>
  </div>
</div>



<script>
let debounceTimer;
const input = document.getElementById('gameSearch');
const dropdown = document.getElementById('suggestions');
const noResults = document.getElementById('noResults');
let currentQuery = '';
let currentSort = 'title'; // NEU: Sortier-State

input.addEventListener('input', () => {
  const query = input.value.trim();
  currentQuery = query;
  clearTimeout(debounceTimer);
  if (query.length < 2) {
    dropdown.style.display = 'none';
    noResults.textContent = '';
    return;
  }
  debounceTimer = setTimeout(() => {
    fetch(`/inc/api/search_games.php?q=${encodeURIComponent(query)}`)
      .then(res => res.json())
      .then(data => {
        dropdown.innerHTML = '';
        if (data.length === 0) {
          dropdown.style.display = 'none';
          noResults.textContent = 'Kein passendes Spiel. Mit Enter hinzufügen.';
        } else {
          dropdown.style.display = 'block';
          noResults.textContent = '';
          data.forEach(game => {
            const div = document.createElement('div');
            div.textContent = game.title;
            div.onclick = () => addGameToProfile(game.id);
            dropdown.appendChild(div);
          });
        }
      })
      .catch(err => {
        console.error("Fehler bei der Spielsuche:", err);
      });
  }, 300);
});

input.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && dropdown.style.display === 'none' && currentQuery.length > 2) {
    e.preventDefault();
    openModal(currentQuery);
  }
});

function openModal(title) {
  document.getElementById('newTitle').value = title;
  document.getElementById('modal').style.display = 'block';
}

function closeModal() {
  document.getElementById('modal').style.display = 'none';
}

document.getElementById('newGameForm').addEventListener('submit', (e) => {
  e.preventDefault();
  const payload = {
    title: document.getElementById('newTitle').value,
    min_age: parseInt(document.getElementById('minAge').value),
    min_player: parseInt(document.getElementById('minPlayer').value),
    max_player: parseInt(document.getElementById('maxPlayer').value),
    release_date: document.getElementById('releaseDate').value
  };

 
  fetch('/inc/api/create_game_and_add.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(res => res.json())
  .then(data => {
    if (data.success) {
      notify('Spiel erstellt & hinzugefügt!');

      closeModal();
      input.value = '';
      loadUserGames();
    } else if (data.error === 'Spiel bereits verknüpft') {
      notify('Dieses Spiel ist bereits in deiner Sammlung.');
      closeModal();
    } else {
      notify('Fehler: ' + (data.error || 'Unbekannt'));
    }
  })
  .catch(err => {
    console.error("Fehler beim Erstellen des Spiels:", err);
  });
});

function addGameToProfile(gameId) {
  fetch('/inc/api/add_game_to_profile.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ boardgame: gameId })
  })
  .then(res => res.json())
  .then(data => {
    if (data.success || data.id) {
      notify('Spiel hinzugefügt!');
      input.value = '';
      dropdown.style.display = 'none';
      loadUserGames();
    } else if (data.error === 'Spiel bereits verknüpft') {
      notify('Dieses Spiel ist bereits in deiner Sammlung.');
    } else {
      notify('Fehler: ' + (data.error || 'Unbekannt'));
    }
  })
  .catch(err => {
    console.error("Fehler beim Hinzufügen:", err);
  });
}

let currentPage = 1;

// NEU: Sortier-Button Event Listener
document.getElementById('sortTitle').addEventListener('click', function() {
  this.classList.add('active');
  document.getElementById('sortRating').classList.remove('active');
  currentSort = 'title';
  loadUserGames(1);
});

document.getElementById('sortRating').addEventListener('click', function() {
  this.classList.add('active');
  document.getElementById('sortTitle').classList.remove('active');
  currentSort = 'rating';
  loadUserGames(1);
});

function loadUserGames(page = 1) {
  const selectedState = document.getElementById('filterState').value;
  const selectedRating = document.getElementById('filterRating').value;
  const selectedSale = document.getElementById('filterSale').value;
  const titleQuery = document.getElementById('filterTitle').value.trim();

   const params = new URLSearchParams({
    page: page,
  });

  if (selectedState) params.append('state', selectedState);
  if (selectedRating && parseFloat(selectedRating) > 0) {
    params.append('min_rating', selectedRating);
  }

  if (selectedSale === "1") {
    params.append('for_sale', 'false');
  } else if (selectedSale === "2") {
    params.append('for_sale', 'true');
  }

  if (titleQuery) params.append('title', titleQuery);
  
  // NEU: Sortierung hinzufügen
  params.append('sort_by', currentSort);

   fetch('/inc/api/list_user_games.php?' + params.toString())
    .then(async res => {
      const text = await res.text();
      let data;

      try {
        data = JSON.parse(text);
      } catch (err) {
        console.error("❌ Fehler beim Parsen der JSON-Antwort:", err);
        return;
      }

      const games = data.results;
      const pagination = data.pagination;
      currentPage = pagination?.page || 1;

      const list = document.getElementById('gameList');
      list.innerHTML = '';

      if (!Array.isArray(games) || games.length === 0) {
        list.innerHTML = '<li>Keine Spiele eingetragen.</li>';
      } else {
       games.forEach(game => {
  const li = document.createElement('li');
  li.className = 'game-item';

  const saleIcon = document.createElement('i');
  saleIcon.className = 'fa-solid fa-tag sale-icon';
  saleIcon.title = game.forSale
  ? 'Verkaufsdaten bearbeiten'
  : 'Zum Verkauf anbieten';
  if (game.forSale) {
    saleIcon.classList.add('active');
  }
  li.appendChild(saleIcon);

  saleIcon.addEventListener('click', () => openSaleModal(game));


  const left = document.createElement('div');
  left.className = 'game-left';

  const title = document.createElement('strong');
  title.className = 'game-title';
  title.textContent = game.title;

  const stars = createStarRating(game.id, parseFloat(game.rating || 0));

  const controls = document.createElement('div');
  controls.className = 'game-controls';

  const status = document.createElement('select');
  status.className = 'status-select';
  status.dataset.id = game.id;

  ['none', 'wishlist', 'played', 'owned'].forEach(value => {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = {
      none: 'Keine',
      wishlist: 'Will spielen',
      played: 'Habe gespielt',
      owned: 'Besitze'
    }[value];
    if (game.state === value) option.selected = true;
    status.appendChild(option);
  });

  status.addEventListener('change', () => {
    updateUserGame(game.id, { state: status.value });
  });

  const removeBtn = document.createElement('button');
  removeBtn.className = 'remove-btn';
  removeBtn.innerHTML = '🗙';
  removeBtn.title = 'Aus Sammlung entfernen';

  removeBtn.addEventListener('click', () => {
    if (!confirm("Willst du dieses Spiel wirklich entfernen?")) return;
    fetch('/inc/api/delete-user-game.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: game.id })
    })
      .then(res => res.text())
      .then(text => {
        let result;
        try {
          result = JSON.parse(text);
        } catch (err) {
          notify("Antwort war kein gültiges JSON: " + text);
          return;
        }
        if (result.success) {
          notify('Spiel entfernt!');
          loadUserGames(currentPage);
        } else {
          notify('Fehler beim Löschen: ' + (result.error || 'Unbekannt'));
        }
      })
      .catch(err => {
        console.error("❌ Netzwerklöschen fehlgeschlagen:", err);
      });
  });

  controls.appendChild(stars);
  controls.appendChild(status);
  controls.appendChild(removeBtn);

  left.appendChild(title);
  li.appendChild(left);
  li.appendChild(controls);

  list.appendChild(li);
});

      }

      renderPagination(pagination);
    })
    .catch(error => {
      console.error("Fetch-Fehler beim Laden der Spiele:", error);
    });
}


function renderPagination(pagination) {
  const containers = [
    document.getElementById('pagination_top'),
    document.getElementById('pagination_bottom')
  ];

  if (!pagination) return;

  const { page, pageCount } = pagination;

  containers.forEach(wrapper => {
    if (!wrapper) return;

    wrapper.innerHTML = '';

    for (let i = 1; i <= pageCount; i++) {
      const btn = document.createElement('button');
      btn.textContent = i;
      btn.className = (i === page) ? 'active' : '';
      btn.addEventListener('click', () => loadUserGames(i));
      wrapper.appendChild(btn);
    }
  });
}



function createStarRating(userGameId, currentRating) {
  const wrapper = document.createElement('div');
  wrapper.classList.add('stars');
  wrapper.dataset.id = userGameId;

  for (let i = 1; i <= 5; i++) {
    const star = document.createElement('span');
    star.classList.add('star');

    if (currentRating >= i) {
      star.classList.add('full');
    }

    star.addEventListener('click', () => {
      console.log("Stern geklickt:", i);
      setRating(userGameId, i);
    });

    wrapper.appendChild(star);
  }

  return wrapper;
}


function setRating(userGameId, ratingValue) {
  updateUserGame(userGameId, { rating: ratingValue });
  console.log("setRating aufgerufen:", userGameId, ratingValue);

  const wrapper = document.querySelector(`.stars[data-id="${userGameId}"]`);
  if (wrapper) {
    wrapper.querySelectorAll('.star').forEach((star, index) => {
      const i = index + 1;
      star.classList.remove('full', 'half');
      if (ratingValue >= i) {
        star.classList.add('full');
      } else if (ratingValue >= i - 0.5) {
        star.classList.add('half');
      }
    });
  }
}

function updateUserGame(userGameId, data) {
  fetch(`/inc/api/update_user_game.php`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: userGameId,
      update: data
    })
  })
  .then(async res => {
    const text = await res.text();
    console.log("API raw response:", text);

    try {
      const json = JSON.parse(text);
      if (json.success) {
        console.log("Spiel aktualisiert:", json);
      } else {
        notify("Fehler: " + (json.error || "Unbekannter Fehler"));
      }
    } catch (err) {
      console.error("Antwort ist kein gültiges JSON:", err);
      notify("Antwort ist ungültig: " + text.slice(0, 100));
    }
  })
  .catch(err => {
    console.error("Fetch-/Netzwerkfehler:", err);
    notify("Verbindungsfehler: " + err.message);
  });
}
document.addEventListener('DOMContentLoaded', function () {
  loadUserGames();
});


function openSaleModal(game) {
  const overlay = document.getElementById('saleModalOverlay');
  const title = document.getElementById('saleModalTitle');

  overlay.dataset.gameId = game.id;

  title.textContent = `${game.title} zum Verkauf anbieten`;

  document.getElementById('saleStatus').value = game.forSale ? "true" : "false";
  document.getElementById('salePrice').value = game.price || '';
  document.getElementById('saleCondition').value = game.condition || 'Good';
  document.getElementById('saleDescription').value = game.description || '';
  document.getElementById('saleDelivery').value = game.deliveryOption || 'ShippingOrPickup';
  document.getElementById('saleTradePossible').checked = !!game.tradePossible;

  overlay.style.display = 'flex';
  document.body.style.overflow = 'hidden';
}

function closeSaleModal() {
  document.getElementById('saleModalOverlay').style.display = 'none';
  document.body.style.overflow = '';
}

document.getElementById('saleForm').addEventListener('submit', (e) => {
  e.preventDefault();

  const overlay = document.getElementById('saleModalOverlay');
const gameId = overlay.dataset.gameId;


  const data = {
    forSale: document.getElementById('saleStatus').value === 'true',
    price: parseFloat(document.getElementById('salePrice').value) || 0,
    condition: document.getElementById('saleCondition').value,
    description: document.getElementById('saleDescription').value.trim(),
    deliveryOption: document.getElementById('saleDelivery').value,
    tradePossible: document.getElementById('saleTradePossible').checked,
  };

  fetch('/inc/api/update_user_game.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: gameId,
      update: data
    })
  })
    .then(async res => {
      const text = await res.text();
      let json;
      try {
        json = JSON.parse(text);
      } catch (err) {
        notify('Antwort ist kein gültiges JSON: ' + text);
        return;
      }

      if (json.success) {
        notify('Verkaufsdaten gespeichert!');
        closeSaleModal();
        loadUserGames();
      } else {
        notify('Fehler: ' + (json.error || 'Unbekannt'));
      }
    })
    .catch(err => {
      console.error('❌ Speichern fehlgeschlagen:', err);
      notify('Netzwerkfehler: ' + err.message);
    });
});


document.getElementById('filterState').addEventListener('change', loadUserGames);
document.getElementById('filterRating').addEventListener('change', loadUserGames);
document.getElementById('filterSale').addEventListener('change', loadUserGames);
document.getElementById('filterTitle').addEventListener('input', () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => loadUserGames(), 300);
});



</script>    
<?php include("../inc/footer.php"); ?>