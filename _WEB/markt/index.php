<?php
include("../inc/header_auth.php");
$jwt = $_SESSION['jwt'];
?>

<style>

/* Filterbereich */
.filters {
  margin-bottom: 1rem;
}
.filters label {
  font-weight: bold;
  margin-right: 10px;
}
#filterTitle {
  padding: 6px;
  width: 250px;
  border-radius: 4px;
  border: 1px solid #444;
  background: #1c242d;
  color: #f0f0f0;
}

/* Markt-Liste */
#marketList {
  list-style: none;
  padding: 0;
  margin: 0;
}

.market-item {
  background: #1c242d;
  color: #f0f0f0;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 6px;
  border: 1px solid #2a3542;
}

.market-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.market-header strong {
  font-size: 1.1rem;
}

.market-header small {
  color: #ccc;
}

.toggle {
  background: #fec50d;
  color: #1c242d;
  border: none;
  font-size:20px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  width: 28px;
  height: 28px;
}

.offers {
  margin-top: 10px;
  display: none;
  border-top: 1px solid #333;
  padding-top: 10px;
}

.offer {
  background: #252d38;
  padding: 8px 10px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.offer strong {
  color: #fec50d;
}

.offer a{
  color:#fec50d;
  text-decoration: none;
}

.offer small {
  color: #aaa;
}

@media (max-width: 768px) {
  #filterTitle {
    width: 100%;
    margin-bottom: 10px;
  }
}
</style>

<div class="content content-plain top-margin">
  <h2>Trödelmarkt</h2>
  <p>Hier findest du alle Spiele, die derzeit von anderen Spielern zum Verkauf angeboten werden.</p>

  <div class="filters">
    <label>
      Titel:
      <input type="text" id="filterTitle" placeholder="Nach Spiel suchen..." />
    </label>
  </div>

  <ul id="marketList"></ul>
</div>

<script>
let debounceTimer;

document.addEventListener('DOMContentLoaded', function () {
  loadMarketOffers();

  document.getElementById('filterTitle').addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => loadMarketOffers(), 400);
  });
});

function loadMarketOffers() {
  const titleQuery = document.getElementById('filterTitle').value.trim();
  const params = new URLSearchParams();
  if (titleQuery) params.append('title', titleQuery);

  fetch('/inc/api/list_market_offers.php?' + params.toString())
    .then(res => res.json())
    .then(data => renderMarket(data.results))
    .catch(err => console.error('Fehler beim Laden der Angebote:', err));
}

function renderMarket(games) {
  const list = document.getElementById('marketList');
  list.innerHTML = '';

  if (!games || games.length === 0) {
    list.innerHTML = '<li>Keine Angebote gefunden.</li>';
    return;
  }

  games.forEach(item => {
    const li = document.createElement('li');
    li.classList.add('market-item');
    const offers = item.offers.map(o => {
  const userLink = o.slug 
    ? `<a href="https://dev.meply.de/user/${o.slug}" >${o.user}</a>` 
    : o.user;

  return `
    <div class="offer">
      <strong>${userLink}</strong> 
      ${o.price ? `– ${o.price} €` : ''}<br>
      <small>${o.condition || 'Zustand unbekannt'} • ${o.delivery || 'keine Angabe'}</small>
      ${o.tradePossible ? '<br><small>🔁 Tausch möglich</small>' : ''}
      ${o.description ? `<br><small>${o.description}</small>` : ''}
    </div>
  `;
}).join('');


    li.innerHTML = `
      <div class="market-header">
        <div>
          <strong>${item.title}</strong><br>
          <small>${item.offers.length} Anbieter</small>
        </div>
        <button class="toggle">+</button>
      </div>
      <div class="offers">${offers}</div>
    `;

const toggleBtn = li.querySelector('.toggle');
const offersDiv = li.querySelector('.offers');
const header = li.querySelector('.market-header'); // ⬅️ nur dieser Bereich reagiert auf Klicks

header.style.cursor = 'pointer';
header.addEventListener('click', (e) => {
  // Keine doppelten Klicks, wenn jemand direkt auf das Symbol klickt
  //if (e.target === toggleBtn) return;

  const visible = offersDiv.style.display === 'block';
  offersDiv.style.display = visible ? 'none' : 'block';
  toggleBtn.textContent = visible ? '+' : '–';
});


    list.appendChild(li);
  });
}
</script>

<?php include("../inc/footer.php"); ?>
