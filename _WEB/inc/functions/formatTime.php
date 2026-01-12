<?php
/**
 * Formatiert einen Zeit-String sicher in das 'H:i:s.v' Format (mit Millisekunden).
 *
 * Nutzt die DateTime-Klasse, um verschiedene Eingabeformate zu parsen
 * (z.B. '14:30' oder '14:30:45') und fängt ungültige Zeiten ab.
 *
 * @param string|null $time Der eingegebene Zeit-String.
 * @return string|null Der formatierte String (z.B. '14:30:00.000')
 * oder null, wenn die Eingabe leer oder ungültig war.
 */
function formatTime(?string $time): ?string {
    // Bei leerer Eingabe direkt null zurückgeben
    if (empty($time)) return null;

    // Versuche, die Zeit zu parsen
    try {
        // DateTime kann '14:30', '14:30:45' etc. korrekt parsen.
        $d = new DateTime($time);
        
        // Formatieren in das Zielformat (Stunde:Minute:Sekunde.Millisekunde)
        return $d->format('H:i:s.v');
        
    } catch (Exception $e) {
        // Fängt ungültige Zeiten (z.B. '99:99' oder 'abc') ab
        return null;
    }
}