<?php

/**
 * Formatiert einen Datums-String sicher in das 'Y-m-d' Format.
 *
 * Diese robuste Helferfunktion nutzt die DateTime-Klasse, um
 * verschiedene Eingabeformate zu parsen. Sie fängt alle Fehler
 * (z.B. bei ungültigen Formaten wie 'abc') sicher ab.
 *
 * @param string|null $date Der eingegebene Datums-String (z.B. '25.10.2025', '2025-10-25', 'gestern').
 * @return string|null Der formatierte String (z.B. '2025-10-25')
 * oder null, wenn die Eingabe leer oder ungültig war.
 */
function formatDate($date) {
    // Bei leerer Eingabe direkt null zurückgeben
    if (empty($date)) return null;
    
    // Versuche, das Datum zu parsen
    try {
        $d = new DateTime($date);
        return $d->format('Y-m-d');
    } catch (Exception $e) {
        // Fängt ungültige Daten (z.B. '30.02.2025' or 'abc') ab
        return null;
    }
}