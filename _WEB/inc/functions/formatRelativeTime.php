<?php
/**
 * Formatiert einen Zeitstempel relativ zur aktuellen Zeit (z.B. "vor 2 Stunden")
 *
 * @param string $datetime ISO 8601 Datetime String (z.B. "2025-01-04T14:30:00.000Z")
 * @return string Formatierter relativer Zeitstempel
 */
function formatRelativeTime($datetime) {
    if (empty($datetime)) {
        return 'Datum unbekannt';
    }

    try {
        $dt = new DateTime($datetime);
        $now = new DateTime();
        $diff = $now->diff($dt);

        // Zukunft (sollte nicht vorkommen, aber sicherheitshalber)
        if ($diff->invert === 0) {
            return $dt->format('d.m.y H:i');
        }

        // Berechne Gesamtdifferenz in verschiedenen Einheiten
        $totalSeconds = $now->getTimestamp() - $dt->getTimestamp();
        $totalMinutes = floor($totalSeconds / 60);
        $totalHours = floor($totalSeconds / 3600);
        $totalDays = floor($totalSeconds / 86400);

        // Weniger als 1 Minute
        if ($totalMinutes < 1) {
            return 'gerade eben';
        }

        // Weniger als 1 Stunde (zeige Minuten)
        if ($totalMinutes < 60) {
            return $totalMinutes === 1 ? 'vor 1 Minute' : "vor {$totalMinutes} Minuten";
        }

        // Weniger als 24 Stunden (zeige Stunden)
        if ($totalHours < 24) {
            return $totalHours === 1 ? 'vor 1 Stunde' : "vor {$totalHours} Stunden";
        }

        // Gestern
        if ($totalDays === 1) {
            return 'Gestern um ' . $dt->format('H:i');
        }

        // Weniger als 7 Tage (zeige Tage)
        if ($totalDays < 7) {
            return "vor {$totalDays} Tagen";
        }

        // Älter als 7 Tage: Absolutes Datum
        return $dt->format('d.m.y');

    } catch (Exception $e) {
        return 'Datum unbekannt';
    }
}
