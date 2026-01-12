<?php
/**
 * Prüft, ob ein String dem Format einer Strapi documentId entspricht
 * (24 Zeichen, bestehend aus Kleinbuchstaben a-z und Ziffern 0-9).
 *
 * @param string $id Der zu prüfende String.
 * @return bool True, wenn das Format passt, sonst false.
 */
function isDocumentId(string $id): bool {
    // Prüft auf genau 24 Zeichen, nur Kleinbuchstaben (a-z) und Ziffern (0-9).
    return preg_match('/^[a-z0-9]{24}$/', $id) === 1;
}