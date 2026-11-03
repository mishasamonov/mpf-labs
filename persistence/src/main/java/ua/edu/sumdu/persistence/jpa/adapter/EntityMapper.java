package ua.edu.sumdu.persistence.jpa.adapter;

import org.springframework.data.domain.Sort;
import sumdu.edu.ua.core.domain.*;
import ua.edu.sumdu.persistence.jpa.entity.*;

/** Entities never leave the persistence module: adapters return core records. */
final class EntityMapper {
  private EntityMapper() {}

  static Book book(BookEntity e) {
    return new Book(e.getId(), e.getTitle(), e.getAuthor(), e.getYear());
  }

  static User user(UserEntity e) {
    return new User(e.getId(), e.getEmail(), e.getPassword(), e.getNickname(), e.getRole(),
        e.isVerified(), e.getVerificationToken());
  }

  static Comment comment(CommentEntity e) {
    // getId() on a lazy proxy returns the key without loading the row.
    Long userId = e.getUser() == null ? null : e.getUser().getId();
    return new Comment(e.getId(), e.getBook().getId(), userId, e.getAuthor(), e.getText(),
        e.getCreatedAt());
  }

  static UserComment userComment(CommentEntity e) {
    return new UserComment(e.getId(), e.getText(), e.getCreatedAt(), book(e.getBook()));
  }

  /** "pubYear,desc" -> Sort by entity property; id is the stable tie-breaker. */
  static Sort sort(String value, Sort.Direction tieBreak) {
    String[] parts = value.split(",");
    String property = switch (parts[0]) {
      case "pubYear" -> "year";
      case "title", "author", "createdAt" -> parts[0];
      default -> "id";
    };
    var direction = parts[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort sort = Sort.by(direction, property);
    return property.equals("id") ? sort : sort.and(Sort.by(tieBreak, "id"));
  }

  static org.springframework.data.domain.PageRequest pageable(PageRequest r, Sort sort) {
    return org.springframework.data.domain.PageRequest.of(r.page(), r.size(), sort);
  }
}
