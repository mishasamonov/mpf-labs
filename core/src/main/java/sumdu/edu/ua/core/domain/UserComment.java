package sumdu.edu.ua.core.domain;

import java.time.Instant;

/** Comment of one user together with the commented book (user comments page). */
public record UserComment(long id, String text, Instant createdAt, Book book) {}
