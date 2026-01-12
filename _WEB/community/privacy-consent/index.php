<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../../inc/functions.php'); // Nötig für requireLogin, getCurrentPrivacyVersion, esc

// 1. SICHERHEIT: Login erzwingen
// Diese Seite darf nur von eingeloggten Nutzern gesehen werden.
requireLogin();

include('../../inc/header.php');
?>

<div class="content content-plain top-margin">
    <?php
    $currentVersion = getCurrentPrivacyVersion();
    // Sicherer Zugriff auf verschachtelte Session-Daten
    $userVersion = $_SESSION['profile']['lastAcceptedPrivacyVersion'] ?? null;

    // Schutz: Nur prüfen, wenn API-Aufruf erfolgreich war UND User-Version bekannt ist
    if ($currentVersion !== null && $userVersion !== null) {
        // Robuster String-Vergleich
        if (trim((string)$currentVersion) !== trim((string)$userVersion)) {
    ?>
        <div class="card">
            <h2>Aktualisierte Datenschutzerklärung (Version <?= esc($currentVersion) ?>)</h2>
            <p>Unsere Datenschutzerklärung wurde aktualisiert. Bitte lies sie durch und bestätige, dass du sie gelesen und akzeptierst.</p>

            <p><a href="<?= esc(WEBSITE_BASE) ?>/datenschutzerklaerung" target="_blank">Zur Datenschutzerklärung</a></p>

            <form method="POST" action="save.php">
                <input type="hidden" name="version" value="<?= esc($currentVersion) ?>">
                <button type="submit">Ich stimme zu</button>
            </form>
        </div>
    <?php
        }
    }
    ?>

    <div class="card">
        <?php
        $deleteDate = $_SESSION['profile']['deleteDate'] ?? null;
        if (empty($deleteDate)):
        ?>
            <h2>Konto zur Löschung vormerken</h2>
            <p>Wenn du der Datenschutzerklärung nicht zustimmen möchtest oder dein Konto aus anderen Gründen löschen möchtest, kannst du dein Konto zur Löschung vormerken.</p>
            <form action="schedule-delete.php" method="POST">
                 <button style="position: inherit;" type="submit" class="btn btn-danger">
                    Konto zur Löschung vormerken
                </button>
            </form>
        <?php else: ?>
            <h2>Konto zur Löschung vorgemerkt</h2>
            <p>Dein Konto ist zur Löschung vorgemerkt.<br>
            <?php
                // Robustes Datums-Parsing
                $formattedDate = 'unbekannt';
                try {
                    $dt = new DateTime($deleteDate);
                    $formattedDate = $dt->format('d.m.Y');
                } catch (Exception $e) { /* Fallback bleibt 'unbekannt' */ }
            ?>
            Die Löschung erfolgt nach dem <strong><?= esc($formattedDate) ?></strong>, sobald der automatische Löschprozess ausgeführt wird.<br>
            In der Regel passiert dies in der Nacht nach diesem Datum. Bis zur tatsächlichen Löschung kannst du dein Konto jederzeit wiederherstellen.</p>
            <form action="cancel-delete.php" method="POST">
                 <button style="position: inherit;" type="submit" class="btn btn-secondary">
                    Konto wiederherstellen
                </button>
            </form>
        <?php endif; ?>
    </div>
</div>

<?php include('../../inc/footer.php'); ?>