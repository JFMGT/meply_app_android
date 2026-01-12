<?php
/**
 * Sicherer HTML-Escape – wandelt alle gefährlichen Zeichen um.
 * Schützt zuverlässig gegen XSS in Text- und Attributkontexten.
 *
 * @param string|null $value     Der zu filternde String
 * @return string                Escapeter (sicherer) HTML-String
 */
function esc(?string $value): string {
    return htmlspecialchars($value ?? '', ENT_QUOTES, 'UTF-8');
}
