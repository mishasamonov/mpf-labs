package ua.edu.sumdu.persistence.jpa.adapter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.*;
import sumdu.edu.ua.core.error.NotFoundException;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import ua.edu.sumdu.persistence.jpa.entity.CommentEntity;
import ua.edu.sumdu.persistence.jpa.repository.*;

@Component
@Transactional(readOnly = true)
public class JpaCommentRepositoryAdapter implements CommentRepositoryPort {
  private final CommentRepository comments;
  private final BookRepository books;
  private final UserRepository users;

  public JpaCommentRepositoryAdapter(
      CommentRepository comments, BookRepository books, UserRepository users) {
    this.comments = comments;
    this.books = books;
    this.users = users;
  }

  @Override
  @Transactional
  public Comment add(long bookId, long userId, String author, String text, Instant createdAt) {
    var book = books.findById(bookId).orElseThrow(() -> new NotFoundException("Книгу не знайдено"));
    var user = users.findById(userId)
        .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
    var comment = new CommentEntity(author, text, createdAt);
    book.addComment(comment);
    user.addComment(comment);
    return EntityMapper.comment(comments.save(comment));
  }

  @Override
  public Page<Comment> list(long bookId, String author, Instant since, PageRequest request) {
    Specification<CommentEntity> spec =
        (root, query, cb) -> cb.equal(root.get("book").get("id"), bookId);
    if (author != null && !author.isBlank()) {
      String pattern = "%" + author.toLowerCase(Locale.ROOT)
          .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("author")), pattern, '!'));
    }
    if (since != null)
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), since));
    var pageable = EntityMapper.pageable(request, EntityMapper.sort(request.sort(), Sort.Direction.DESC));
    var page = comments.findAll(spec, pageable);
    return new Page<>(page.map(EntityMapper::comment).getContent(), request, page.getTotalElements());
  }

  @Override
  public Optional<Comment> findById(long bookId, long commentId) {
    return comments.findByIdAndBookId(commentId, bookId).map(EntityMapper::comment);
  }

  @Override
  public boolean bookExists(long bookId) {
    return books.existsById(bookId);
  }

  @Override
  @Transactional
  public Optional<Comment> update(long bookId, long commentId, String author, String text) {
    return comments.findByIdAndBookId(commentId, bookId).map(comment -> {
      comment.setAuthor(author); // Dirty checking writes the UPDATE on commit.
      comment.setText(text);
      return EntityMapper.comment(comment);
    });
  }

  @Override
  @Transactional
  public boolean delete(long bookId, long commentId) {
    return comments.findByIdAndBookId(commentId, bookId).map(comment -> {
      comment.getBook().removeComment(comment);
      comments.delete(comment);
      return true;
    }).orElse(false);
  }

  @Override
  public List<UserComment> findByUserId(long userId) {
    return comments.findByUserId(userId).stream()
        .sorted(Comparator.comparing(CommentEntity::getCreatedAt)
            .thenComparing(CommentEntity::getId).reversed())
        .map(EntityMapper::userComment)
        .toList();
  }
}
