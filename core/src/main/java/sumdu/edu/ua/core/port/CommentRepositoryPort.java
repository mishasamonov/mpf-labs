package sumdu.edu.ua.core.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import sumdu.edu.ua.core.domain.*;

public interface CommentRepositoryPort {
  Comment add(long bookId, long userId, String author, String text, Instant createdAt);

  Page<Comment> list(long bookId, String author, Instant since, PageRequest request);

  Optional<Comment> findById(long bookId, long commentId);

  /** Lab 10: lets CommentService.delete tell a missing book from a missing comment. */
  boolean bookExists(long bookId);

  Optional<Comment> update(long bookId, long commentId, String author, String text);

  boolean delete(long bookId, long commentId);

  /** Newest first, with the commented book. */
  List<UserComment> findByUserId(long userId);
}
