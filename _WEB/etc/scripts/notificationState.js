document.addEventListener("DOMContentLoaded", () => {
  const list = document.getElementById("notificationList");

  if (!list) return;

  // Delegierter Click-Handler für dynamisch geladene <a>
  list.addEventListener("click", async (e) => {
    const link = e.target.closest("a[data-id]");
    if (!link) return;

    e.preventDefault(); // Verhindert Sofortnavigation

    const id = link.dataset.id;
    const url = link.href;

    if (!id) {
      console.warn("Kein data-id gefunden");
      window.location.href = url;
      return;
    }

    try {
      await fetch("/inc/api/markNotificationSeen.php", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id }),
      });
    } catch (err) {
      console.error("Fehler beim Setzen von 'seen':", err);
      // Fehler? Trotzdem weiterleiten
    }

    // Weiterleitung
    window.location.href = url;
  });
});