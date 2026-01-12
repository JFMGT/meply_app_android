document.addEventListener('DOMContentLoaded', function () {
  const toggle = document.getElementById('notificationToggle');
  const dropdown = document.getElementById('notificationDropdown');
  const list = document.getElementById('notificationList');
  const badge = document.getElementById('notificationCount');

  if (!toggle || !dropdown || !list || !badge) return;

  toggle.addEventListener('click', function (e) {
    e.preventDefault();
    dropdown.classList.toggle('hidden');
  });

  document.addEventListener('click', function (e) {
    if (!toggle.contains(e.target) && !dropdown.contains(e.target)) {
      dropdown.classList.add('hidden');
    }
  });

  fetch("/inc/api/notifications.php")
    .then(res => res.json())
    .then(data => {
      list.innerHTML = "";
      const notifications = data.data || [];

      if (!Array.isArray(notifications)) {
        list.innerHTML = `<li><i class="fa-solid fa-triangle-exclamation"></i> Fehlerhafte Daten</li>`;
        badge.classList.add("hidden");
        return;
      }

      const typesShown = new Set();
       console.log(notifications);
      for (let notif of notifications) {

  const { documentId, type, seen, message, sender} = notif;

  if (!seen && !typesShown.has(type)) {
    typesShown.add(type);

    let icon = "fa-circle-info";
    let msg = "Neue Aktivität";
    let link = "#"; // Fallback-Link

    if (type === "pm") {
      icon = "fa-envelope";
      msg = "Du hast neue Nachrichten.";
      link = "/community/messages/";
    } else if (type === "like") {
      icon = "fa-heart";
      msg = "Dein Beitrag wurde geliked.";
      link = "#"; // oder direkt zum Beitrag, falls möglich
    } else if (type === "comment") {
      icon = "fa-comment";
      msg = "Jemand hat geantwortet.";
      link = "#";
    } else if (type === "followed-user-posted") {
      icon = "fa-binoculars";
      msg = "Jemand dem du folgst, hat ein neues Gesuch erstellt.";
      const userslug = sender?.userslug;
      link = `https://dev.meply.de/user/${userslug}`;
    } else if (type === "error") {
      icon = "fa-triangle-exclamation";
      msg = message || "Unbekannter Fehler";
    }

    list.innerHTML += `<li><a data-id="${documentId}" href="${link}"><i class="fa-solid ${icon}"></i> ${msg}</a></li>`;
  }
}


      if (typesShown.size > 0 && !typesShown.has("error")) {
        badge.classList.remove("hidden");
        badge.textContent = "!";
      } else {
        list.innerHTML = `<li><i class="fa-regular fa-bell-slash"></i> Keine neuen Benachrichtigungen</li>`;
        badge.classList.add("hidden");
      }
    })
    .catch(err => {
      console.error("Benachrichtigungen konnten nicht geladen werden:", err);
      list.innerHTML = `<li><i class="fa-solid fa-triangle-exclamation"></i> Fehler beim Laden</li>`;
      badge.classList.add("hidden");
    });
});