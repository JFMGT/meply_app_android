<?php

/**
 * Generiert einen kryptographisch SICHREN Zufalls-String mit definierter Länge.
 *
 * Diese Funktion MUSS `random_int` verwenden, da `rand()` unsicher und vorhersagbar ist.
 *
 * @param int $length Die gewünschte Länge des Strings.
 * @return string Ein zufälliger String.
 */
function generateRandomString($length = 5): string {
    $characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    
    // 1. NEU: Länge außerhalb der Schleife speichern
    $charactersLength = strlen($characters);
    $randomString = '';
    
    for ($i = 0; $i < $length; $i++) {
        // 2. KORRIGIERT: `random_int` (sicher) statt `rand` (unsicher) verwenden
        $randomString .= $characters[random_int(0, $charactersLength - 1)];
    }
    
    return $randomString;
}