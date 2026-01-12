<?php
/**
 * Zeigt das Optionsmenü (Melden/Löschen) für einen Post, Benutzer oder Event an.
 *
 * @param string $documentId Die ID des Eintrags.
 * @param string $postType Der Typ ('post', 'user', 'event').
 * @param bool $isOwner Ob der aktuelle Benutzer der Besitzer ist.
 * @param string $class Zusätzliche CSS-Klassen für den Container.
 * @return string Der gerenderte HTML-Block.
 */
function displayPostOptions($documentId, $postType, $isOwner = false, $class = "") {
    
    switch ($postType) {
    case 'post':
        $reportButtonText = '⚠️ Beitrag melden';
        break;
    case 'user':
        $reportButtonText = '⚠️ Benutzer melden';
        break;
    case 'event':
        $reportButtonText = '⚠️ Event melden';
        break;
    default:
        $reportButtonText = '⚠️ Melden';
        break;
    }

    // -----------------------------------------------------------------
    // SICHERHEITS-FIX (XSS-Schutz)
    // -----------------------------------------------------------------
    // Alle Variablen, die in HTML eingefügt werden, MÜSSEN mit
    // htmlspecialchars() behandelt werden, um XSS-Angriffe zu verhindern.
    // Die Variable $class war vorher ungeschützt.
    
    $safeClass = htmlspecialchars($class, ENT_QUOTES, 'UTF-8');
    $safeDocId = htmlspecialchars($documentId, ENT_QUOTES, 'UTF-8');
    $safeType = htmlspecialchars($postType, ENT_QUOTES, 'UTF-8');


    // -----------------------------------------------------------------
    // ARCHITEKTUR-HINWEIS (HTML in PHP)
    // -----------------------------------------------------------------
    // Das Erstellen von HTML-Strings in PHP-Funktionen ("String-Building")
    // ist schlechte Praxis ("Code Smell").
    //
    // WARUM?
    // 1. Wartung: Änderungen am Design (z.B. CSS-Klassen, Icons)
    //    erfordern eine Änderung an der PHP-Logik.
    // 2. Lesbarkeit: Es ist unübersichtlich und fehleranfällig.
    // 3. Trennung: Es vermischt Anwendungslogik (PHP) mit
    //    Präsentation (HTML).
    //
    // BESSER: Eine Template-Datei (.php) nutzen, die das HTML enthält
    // und PHP nur für die Logik (if/loops) verwendet.
    // -----------------------------------------------------------------
    
    // Erstelle den HTML-Code
    $output = '<div class="item-options '. $safeClass .'">'; // <-- $safeClass wird hier sicher eingefügt
    $output .= '<button class="options-toggle" title="Optionen">⋮</button>';
    $output .= '<div class="options-menu">';

    // Wenn der Benutzer der Besitzer ist, wird die Löschen-Option angezeigt
    if ($isOwner) {
        $output .= '<button class="delete-post" data-documentid="' . $safeDocId . '">🗑 Beitrag löschen</button>';
    }

    // Die Meldung-Option wird immer angezeigt
    $output .= '<button class="report-post" data-type="' . $safeType . '" data-documentid="' . $safeDocId . '">' . $reportButtonText . '</button>';

    $output .= '</div></div>';

    // Gib den HTML-Code zurück
    return $output;
}
?>