<?php
/**
 * Analysiert eine hochgeladene CSV-Datei auf ein Zeilenlimit und
 * prüft auf CSV-Injection-Versuche.
 *
 * WICHTIG: Diese Funktion prüft NICHT auf XSS oder SQL-Injection.
 * Diese Bedrohungen MÜSSEN durch Output-Encoding (htmlspecialchars)
 * und Prepared Statements bei der Verarbeitung der Daten behandelt werden.
 *
 * @param string $filePath Der Pfad zur hochgeladenen Datei (z.B. aus $_FILES['tmp_name'])
 * @param string $allowedBaseDir Das Verzeichnis, in dem sich die Datei befinden MUSS.
 * @param int $maxLines Die maximal erlaubte Anzahl an Zeilen.
 * @return array Ein assoziatives Array mit dem Analyse-Ergebnis.
 */
function analyzeCsvFile(string $filePath, string $allowedBaseDir, int $maxLines = 250): array {
    
    // -----------------------------------------------------------------
    // KRITISCHE SICHERHEITSPRÜFUNG: Path Traversal
    // -----------------------------------------------------------------
    // Wir müssen sicherstellen, dass die Datei sich im erwarteten Upload-Verzeichnis befindet
    // und nicht z.B. '/etc/passwd' ist.

    // realpath() löst Pfade wie '../' auf und gibt den echten, absoluten Pfad zurück.
    // Es gibt 'false' zurück, wenn die Datei oder der Pfad nicht existiert.
    $realAllowedDir = realpath($allowedBaseDir);
    $realFilePath = realpath($filePath);

    // Wenn der Pfad ungültig ist (false) oder nicht mit dem erlaubten Pfad beginnt...
    if ($realFilePath === false || $realAllowedDir === false || strpos($realFilePath, $realAllowedDir) !== 0) {
        
        // Logge den versuchten Verstoß (optional, aber empfohlen)
        debugLog("Sicherheitswarnung: Ungültiger Dateipfad-Zugriff (Path Traversal Versuch?): $filePath");
        
        return [
            'success' => false,
            'message' => 'Ungültiger Dateipfad oder Datei nicht gefunden.'
        ];
    }

    // -----------------------------------------------------------------
    // Relevante Sicherheitsprüfung: CSV-Injection
    // -----------------------------------------------------------------
    // Prüft auf Zellen, die mit =, +, -, @ beginnen.
    // Diese können in Excel/Sheets als Formeln interpretiert werden.
    $csvInjectionPattern = '/^[=+\-@]/';
    
    $lineCount = 0;
    $isSafe = true; // Geht davon aus, dass die Datei sicher ist

    $handle = fopen($realFilePath, "r");
    if (!$handle) {
        return [
            'success' => false,
            'message' => 'Datei konnte nicht zum Lesen geöffnet werden.'
        ];
    }

    while (!feof($handle)) {
        $line = fgets($handle);
        // Überspringe leere Zeilen
        if (trim($line) === "") continue;

        $lineCount++;

        // Prüfung auf Zeilenlimit
        if ($lineCount > $maxLines) {
            break; // Schleife verlassen, wir haben genug gezählt
        }

        $cells = str_getcsv($line);
        foreach ($cells as $cell) {
            $trimmedCell = trim($cell);
            
            // Führe NUR die relevante CSV-Injection-Prüfung durch
            if (preg_match($csvInjectionPattern, $trimmedCell)) {
                $isSafe = false;
                
                // Logge den Fund
                debugLog("Sicherheitswarnung: Potentielle CSV-Injection in Datei $realFilePath, Zeile $lineCount gefunden.");
                
                // Direkt abbrechen und Handle schließen.
                // Das ist viel sauberer als 'break 3;'.
                fclose($handle);
                
                return [
                    'success' => true, // Die Analyse war erfolgreich
                    'isSafe' => false, // ABER die Datei ist unsicher
                    'isUnderLimit' => true, // Wir sind noch im Limit, aber das ist jetzt egal
                    'linesProcessed' => $lineCount,
                    'securityIssue' => 'CSV-Injection'
                ];
            }
        }
    }

    fclose($handle);

    $isUnderLimit = $lineCount <= $maxLines;

    return [
        'success' => true, // Analyse erfolgreich abgeschlossen
        'isSafe' => $isSafe, // true, wenn kein Muster gefunden wurde
        'isUnderLimit' => $isUnderLimit,
        'linesProcessed' => $lineCount,
        'securityIssue' => null
    ];
}