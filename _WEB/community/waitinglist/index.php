<?php 
// ARCHITEKTUR: Sicherstellen, dass Funktionen geladen sind
require_once('../../inc/functions.php'); 
include('../../inc/header.php'); 
?>

<div class="content top-margin content-plain">
    <?php
    $confirmationMessage = '';
    $confirmationStatus = '';

    if (isset($_GET['token'])) {
        // Annahme: confirmWaitlistToken ist in functions.php
        $result = confirmWaitlistToken($_GET['token']);
        $confirmationMessage = $result['message'];
        $confirmationStatus = $result['status'];
    }
    ?>

    <h1>Trage dich in die Warteliste ein</h1>

    <?php if ($confirmationMessage): ?>
        <?php
        $statusClass = ($confirmationStatus === 'success') ? "success" : "info";
        ?>
        <div class="card <?= esc($statusClass); ?>">
            <?= esc($confirmationMessage); ?>
        </div>
    <?php endif; ?>

    <div class="card">
        <p>Deine Region ist noch nicht freigeschaltet? Kein Problem! Trag dich hier in unsere Warteliste ein und wir benachrichtigen dich, sobald Meply auch bei dir startet.</p>

        <div id="waitlist-message" style="margin-top:1rem;"></div>

        <form id="waitlist-form">
            <p>
                <label for="waitlist-email">E-Mail-Adresse</label>
                <input type="email" name="waitlist-email" id="waitlist-email" required>
            </p>
            <p>
                <label for="waitlist-zip">Postleitzahl</label>
                <input type="text" name="waitlist-zip" id="waitlist-zip" maxlength="5" pattern="\d{5}" required>
            </p>
            <p style="font-size:12px">
                Deine E-Mail-Adresse wird ausschließlich für die Benachrichtigung verwendet. Du kannst dich jederzeit wieder abmelden.
            </p>
            <p>
                <input type="submit" value="Eintragen">
            </p>
        </form>
    </div>
    <div class="card info">
        Deine Meinung geändert? <a style="color:black" href="/community/waitinglist/unsubscribe.php">Trag dich hier wieder aus</a>
    </div>
</div>

<script>
document.getElementById('waitlist-form').addEventListener('submit', async function(e) {
    e.preventDefault();

    const email = document.getElementById('waitlist-email').value.trim();
    const zip = document.getElementById('waitlist-zip').value.trim();
    const messageBox = document.getElementById('waitlist-message');
    
    // Reset Stil
    messageBox.style.color = 'inherit';

    if (!email || !zip.match(/^\d{5}$/)) {
        messageBox.style.color = 'red';
        // SICHERHEITS-FIX: textContent statt innerHTML
        messageBox.textContent = '❌ Bitte gib eine gültige E-Mail-Adresse und Postleitzahl ein.';
        return;
    }

    messageBox.style.color = '#666';
    messageBox.textContent = '⏳ Wird übermittelt...';

    try {
        const response = await fetch('/inc/api/waitinglist.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: email,
                zip: zip
            })
        });

        const result = await response.json();

        if (result.success) { // Prüfe auf 'success' Flag vom Server
            messageBox.style.color = 'green';
            messageBox.textContent = result.message || '✅ Bitte bestätige deine E-Mail-Adresse über den Link, den wir dir geschickt haben.';
            document.getElementById('waitlist-form').reset();
        } else {
            messageBox.style.color = 'red';
            // Prüfe auf 'message' ODER 'error' (je nachdem, was die API schickt)
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