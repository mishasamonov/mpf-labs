package sumdu.edu.ua.core.domain;

import java.time.Instant;

/** userId is null when the author account was deleted (ON DELETE SET NULL). */
public record Comment(
    long id, long bookId, Long userId, String author, String text, Instant createdAt) {}
