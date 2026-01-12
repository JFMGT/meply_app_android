<?php
include("../inc/header_auth.php");

$curUser = $_SESSION['user']['documentId'] ?? null;
$jwt = $_SESSION['jwt'] ?? null;

if (!$curUser || !$jwt) {
    echo "<p>❌ Nicht eingeloggt.</p>";
    include("../inc/footer.php");
    exit;
}

?>

<div class="content content-plain top-margin">
  <div class="card">
    <h1>Follower</h1>
    <p>Übersicht über offene Anfragen und Leuten die dir Folgen oder denen du folgst.</p>
  </div>


  <div class="card">
  <h2>🔔 Offene Follow-Anfragen</h2>
  <ul id="pending-list"></ul>
</div>

<div class="card">
  <h2>🧍 Menschen die dir folgen</h2>
  <ul id="followers-list"></ul>
</div>

<div class="card">
  <h2>🙋 Menschen denen du folgst</h2>
  <ul id="following-list"></ul>
</div>

<div class="card">
  <h2>🚫 Blockierte Anfragen</h2>
  <ul id="blocked-list"></ul>
</div>

</div>
<script>
	async function loadFollowLists() {
  try {
    const res = await fetch('/inc/api/follow_list.php');
    const data = await res.json();

    if (!res.ok) throw new Error(data.error || 'Fehler beim Laden');
    console.log(data);
    renderList('pending-list', data.pending, ['accepted', 'declined']);
    renderList('followers-list', data.followers, ['declined']);
    renderList('following-list', data.following, ['unfollow'], 'following');
    renderList('blocked-list', data.blocked, ['accepted']);

    // Buttons neu binden
    bindFollowActions();
  } catch (err) {
    console.error('Fehler:', err);
  }
}

function renderList(containerId, items, actions, type = '') {
  const container = document.getElementById(containerId);
  container.innerHTML = '';

  if (!Array.isArray(items) || items.length === 0) {
    container.innerHTML = '<li>Keine Einträge</li>';
    return;
  }

  items.forEach(item => {
    const li = document.createElement('li');
    const username = escapeHtml(item.user?.username || 'Unbekannt');

    // Default-Follow-Links
    let actionLinks = actions.map(action =>
      `<a href="#" class="follow-action" data-follow-id="${item.id}" data-action="${action}">
        ${action === 'accepted' ? 'Annehmen' :
          action === 'declined' ? 'Ablehnen' :
          action === 'unfollow' ? 'Entfernen' : action}
      </a>`
    ).join(' ');

    // Sonderfall: "following"-Liste → andere Linkstruktur
    if (type === 'following') {
      actionLinks = `
        <a href="#" class="douserfollow" data-document-id="${item.user.documentId}">
          Entfernen
        </a>`;
    }

    li.innerHTML = `${username} ${actionLinks}`;
    container.appendChild(li);
  });
}


function escapeHtml(text) {
  const div = document.createElement('div');
  div.innerText = text;
  return div.innerHTML;
}

function bindFollowActions() {
  document.querySelectorAll('.follow-action').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const followId = btn.dataset.followId;
      const action = btn.dataset.action;
      if (followId && action) {
        manageFollow(followId, action).then(loadFollowLists);
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  loadFollowLists();
});
</script>
<?php include("../inc/footer.php"); ?>