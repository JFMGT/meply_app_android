<?php
/**
 * Hilfsfunktion, um Status anhand updatedAt von Draft und Published zu bestimmen
 *
 * @param array $draft Der Draft-Eintrag.
 * @param array|null $published Der Published-Eintrag.
 * @return string 'draft' oder 'published'.
 */
function determinePostStatus(array $draft, ?array $published = null): string {
    
    // 1. Holen der Zeitstempel (robust, dank '??')
    // Wenn 'updatedAt' fehlt, wird ein leerer String verwendet.
    $draftTime = $draft['updatedAt'] ?? '';
    $publishedTime = $published['updatedAt'] ?? '';

    // 2. Logik (unverändert, aber jetzt mit $publishedTime)
    if (empty($published) || empty($publishedTime)) {
        return 'draft'; // Kein Published vorhanden → Draft
    }

    // 3. String-Vergleich (viel schneller und robuster als strtotime)
    // Ein neuerer Zeitstempel-String ist "größer" als ein älterer.
    if ($draftTime > $publishedTime) {
        return 'draft'; // Draft neuer → Draft aktiv
    }

    return 'published'; // Published aktueller oder gleich
}