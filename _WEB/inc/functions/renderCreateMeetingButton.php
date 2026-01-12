<?php
// Annahme: config.php (für WEBSITE_BASE) und esc.php sind global geladen.
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';

/**
 * Rendert einen Button zum Erstellen einer Mitspielersuche,
 * abhängig vom Login-Status und ob das Event terminiert ist.
 *
 * @param string $dataAttribute Der Name des data-Attributs (z.B. 'location', 'event').
 * @param string $value Der Wert für das data-Attribut (z.B. die documentId).
 * @param bool $terminated Ob das zugehörige Event/Location terminiert ist.
 * @param string $title Optional: Titel des Events/Location für Anzeige im Modal.
 * @return string Das gerenderte HTML.
 */
function renderCreateMeetingButton(string $dataAttribute, string $value, bool $terminated = true, string $title = ''): string {
    // Session-Prüfung (vereinfacht, 'is_logged_in_soft' wäre besser)
    $isLoggedIn = !empty($_SESSION['jwt']) && !empty($_SESSION['profile']);

    $icon = '<i class="fa-solid fa-puzzle-piece"></i>';
    $label = ' Neue Mitspielersuche erstellen';

    // SICHERHEIT: Variablen explizit escapen
    $safeAttribute = esc($dataAttribute);
    $safeValue = esc($value);
    $safeTitle = esc($title);
    $loginUrl = esc(WEBSITE_BASE . '/community/login/');

    if ($isLoggedIn && $terminated) {
        // ✅ SICHERHEIT: data-user Attribut entfernt (Backend setzt author automatisch)
        // Wenn Titel vorhanden, füge data-{attribute}-title hinzu
        $titleAttr = $safeTitle ? " data-{$safeAttribute}-title=\"{$safeTitle}\"" : '';

        return <<<HTML
<div class="card create-meeting-card">
  <a href="#" class="create-meeting" data-{$safeAttribute}="{$safeValue}"{$titleAttr}>
    {$icon}{$label}
  </a>
</div>
HTML;
    } elseif (!$terminated) {
        return <<<HTML
<div class="card create-meeting-card">
  <a>{$icon} Veranstaltung noch nicht terminiert.</a>
</div>
HTML;
    } else { // Nicht eingeloggt
        return <<<HTML
<div class="card create-meeting-card">
  <a href="{$loginUrl}">{$icon} Einloggen, um Mitspielersuche zu erstellen</a>
</div>
HTML;
    }
}
?>