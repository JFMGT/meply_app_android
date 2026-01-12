<?php 
// ARCHITEKTUR: Sicherstellen, dass Funktionen geladen sind
require_once('../../inc/functions.php'); 
include('../../inc/header.php'); 
?>

<div class="content top-margin content-plain">
    <h1>Warteliste verlassen</h1>
    <div class="card">
        <p>Wenn du deine Anmeldung zur Warteliste rückgängig machen möchtest, kannst du dich hier austragen.</p>

        <div id="unsubscribe-message" style="margin-top:1rem;"></div>

        <form id="unsubscribe-form">
            <p>
                <label for="unsubscribe-email">E-Mail-Adresse</label>
                <input type="email" name="unsubscribe-email" id="unsubscribe-email" required>
            </p>
            <p style="font-size:12px">
                Du erhältst eine E-Mail mit einem Link, um dich endgültig auszutragen.
            </p>
            <p>
                <input type="submit" value="Austragen">
            </p>
        </form>
    </div>
</div>

<script>
document.getElementById('unsubscribe-form').addEventListener('submit', async function(e) {
    e.preventDefault();

    const email = document.getElementById('unsubscribe-email').value.trim();
    const messageBox = document.getElementById('unsubscribe-message');
    
    // Reset Stil
    messageBox.style.color = 'inherit';

    if (!email) {
        messageBox.style.color = 'red';
        // SICHERHEITS-FIX: textContent statt innerHTML
        messageBox.textContent = '❌ Bitte gib deine E-Mail-Adresse an.';
        return;
    }

    messageBox.style.color = '#666';
    messageBox.textContent = '⏳ Anfrage wird verarbeitet...';

    try {
        const response = await fetch('/inc/api/unsubscribe_user.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: email })
        });

        // Wir erwarten JSON, also direkt parsen
        const result = await response.json();

        if (result.success) { // Prüfe auf 'success' Flag
            messageBox.style.color = 'green';
            messageBox.textContent = result.message;
            document.getElementById('unsubscribe-form').reset();
        } else {
            messageBox.style.color = 'red';
            // Prüfe auf 'message' ODER 'error'
            messageBox.textContent = result.message || result.error || '❌ Ein Fehler ist aufgetreten.';
        }
    } catch (err) {
        console.error(err);
        messageBox.style.color = 'red';
        messageBox.textContent = '❌ Serverfehler. Bitte versuche es später erneut.';
    }
});
</script>

<?php include("../../inc/footer.php"); ?>