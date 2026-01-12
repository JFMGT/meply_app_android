<?php
/**
 * Versucht, verschiedene Datums-String-Formate in das Standardformat 'Y-m-d' umzuwandeln.
 *
 * @param string|null $input Der eingegebene Datums-String.
 * @return string|null Das Datum im 'Y-m-d'-Format oder null bei Fehler/Ungültigkeit.
 */
function normalizeDate(?string $input): ?string {
    if (empty($input)) {
        return null;
    }

    // 1. Versuche die häufigsten Formate zuerst (ISO und Deutsch)
    $formatsToTry = [
        'Y-m-d', // ISO (Priorität 1)
        'd.m.Y', // Deutsch
        'd-m-Y', // Deutsch mit Bindestrich (Bugfix)
        'Y',     // Nur Jahr
    ];

    foreach ($formatsToTry as $format) {
        $date = DateTime::createFromFormat($format, $input);
        
        // WICHTIG: Prüfen, ob das geparste Datum auch wirklich Sinn ergibt
        // createFromFormat('Y', '1982-12-04') gibt z.B. fälschlicherweise ein Datum zurück!
        // Wir prüfen, ob die Formatierung zurück zum Original (oder Teilen davon) passt.
        if ($date !== false) {
             // Bei 'Y' (nur Jahr) ist die Prüfung einfacher
            if ($format === 'Y' && $date->format('Y') === $input) {
                return $date->format('Y-01-01'); // Standard auf 1. Jan
            } 
            // Bei vollen Datumsformaten, prüfe ob das Re-Formatieren passt
            elseif ($format !== 'Y' && $date->format($format) === $input) {
                // Zusätzliche Prüfung für 'd-m-Y', um sicherzustellen, dass es nicht fälschlicherweise gematcht wurde
                if ($format === 'd-m-Y') {
                     $checkDate = new DateTime($input); // Versuche, es generisch zu parsen
                     if ($checkDate && $checkDate->format('Y-m-d') === $date->format('Y-m-d')) {
                         return $date->format('Y-m-d');
                     }
                     // Wenn die generische Prüfung fehlschlägt, war es wahrscheinlich kein d-m-Y
                     continue; 
                }
                return $date->format('Y-m-d');
            }
        }
    }

    // 2. Fallback: Versuche generisch zu parsen (fängt viele andere Formate ab)
    try {
        $date = new DateTime($input);
        // Prüfen, ob das Jahr Sinn ergibt (verhindert sehr alte oder zukünftige Daten)
        $year = (int)$date->format('Y');
        if ($year > 1900 && $year < 2100) {
             return $date->format('Y-m-d');
        }
    } catch (Exception $e) {
        // Parsing fehlgeschlagen
    }

    // Wenn nichts funktioniert hat
    return null;
}