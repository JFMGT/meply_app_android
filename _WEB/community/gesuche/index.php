<?php
// ARCHITEKTUR: header_auth.php sollte zentral functions.php laden und requireLogin() aufrufen.
include("../../inc/header_auth.php");
?>

<div class="content content-plain top-margin">
    <div class="card">
        <h1>Deine Spielergesuche</h1>
        <p>Eine Übersicht über deine Gesuche.</p>
    </div>

    <?php
    // 1. Profil-ID sicher holen
    // getProfileData(true) erzwingt ggf. ein Neuladen, falls nötig.
    // Normalerweise reicht getProfileData(), wenn header_auth es schon geladen hat.
    $profileID = getProfileData();

    if ($profileID) {
        // 2. Button rendern (Nutzt jetzt die sichere Version)
        echo renderCreateMeetingButton('user', $profileID);

        // 3. Meetings rendern (Nutzt jetzt die sichere & performante Version)
        // 'author' Filter zeigt nur eigene Gesuche.
        echo get_meetings('author', $profileID);
    } else {
        // Fallback, falls Profil nicht geladen werden konnte
        echo '<div class="card error">Fehler beim Laden des Profils.</div>';
    }
    ?>
</div>

<?php include("../../inc/footer.php"); ?>