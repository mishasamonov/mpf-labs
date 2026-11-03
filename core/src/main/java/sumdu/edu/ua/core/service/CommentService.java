package sumdu.edu.ua.core.service;

import jakarta.transaction.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import sumdu.edu.ua.core.domain.*;
import sumdu.edu.ua.core.error.*;
import sumdu.edu.ua.core.port.CommentRepositoryPort;

@Transactional
public class CommentService {
  public static final Duration DELETE_WINDOW = Duration.ofHours(24);
  public static final int MAX_TEXT_LENGTH = 1000;
  /** Lab 10 (extension): words that may not appear in a comment, compared case-insensitively. */
  public static final Set<String> FORBIDDEN_WORDS = Set.of("спам", "казино", "ставки", "spam", "casino");

  private final CommentRepositoryPort repo;
  private final CatalogService catalog;
  private final UserService users;
  private final Clock clock;

  /** The constructor of the lab 10 methodology: enough for delete(...) and validateText(...). */
  public CommentService(CommentRepositoryPort repo) {
    this(repo, Clock.systemUTC());
  }

  /** Same as above with a fixed clock for unit tests of the 24-hour rule. */
  public CommentService(CommentRepositoryPort repo, Clock clock) {
    this(repo, null, null, clock);
  }

  public CommentService(
      CommentRepositoryPort repo, CatalogService catalog, UserService users, Clock clock) {
    this.repo = Objects.requireNonNull(repo, "repo");
    this.catalog = catalog;
    this.users = users;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** The author is the signed-in account (nickname), never a value from the form. */
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Comment add(long bookId, long userId, String text) {
    catalog().get(bookId);
    String checked = validateText(text);
    User user = users().findById(userId);
    return repo.add(bookId, user.getId(), user.getNickname(), checked, clock.instant());
  }

  /**
   * Lab 10 (extension): trims the text and rejects blank, too long or forbidden content
   * with InvalidCommentTextException.
   */
  public String validateText(String text) {
    if (text == null || text.isBlank())
      throw new InvalidCommentTextException("Текст коментаря не може бути порожнім");
    String trimmed = text.trim();
    if (trimmed.length() > MAX_TEXT_LENGTH)
      throw new InvalidCommentTextException("Текст коментаря довший за " + MAX_TEXT_LENGTH
          + " символів (" + trimmed.length() + ")");
    for (String word : trimmed.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
      if (FORBIDDEN_WORDS.contains(word))
        throw new InvalidCommentTextException("Коментар містить заборонене слово «" + word + "»");
    return trimmed;
  }

  public List<UserComment> getCommentsByUser(long userId) {
    users().findById(userId);
    return repo.findByUserId(userId);
  }

  public Page<Comment> list(long bookId, String author, Instant since, PageRequest request) {
    catalog().get(bookId);
    if (author != null && author.length() > 64)
      throw new ValidationException("Автор: не більше 64 символів");
    if (!(request.sort().startsWith("id,")
        || request.sort().startsWith("createdAt,")
        || request.sort().startsWith("author,")))
      throw new ValidationException("Некоректне сортування відгуків");
    return repo.list(bookId, author == null ? null : author.trim(), since, request);
  }

  public Comment get(long bookId, long commentId) {
    catalog().get(bookId);
    if (commentId <= 0) throw new ValidationException("commentId має бути додатним");
    return repo.findById(bookId, commentId)
        .orElseThrow(() -> new NotFoundException("Відгук не знайдено"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Comment update(long bookId, long commentId, String author, String text) {
    get(bookId, commentId);
    return repo.update(bookId, commentId,
        CatalogService.clean(author, 64, "Автор"), validateText(text))
        .orElseThrow(() -> new NotFoundException("Відгук не знайдено"));
  }

  /**
   * Lab 10: validates the arguments first and calls the repository port only when they are
   * correct. createdAt comes from the form and is compared with the stored value, so the
   * 24-hour rule cannot be bypassed by editing the hidden field.
   */
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(long bookId, long commentId, Instant createdAt) {
    if (bookId <= 0 || commentId <= 0)
      throw new InvalidCommentDeleteException(
          "Ідентифікатори книги та коментаря мають бути додатними (bookId=" + bookId
              + ", commentId=" + commentId + ")");
    if (createdAt == null)
      throw new InvalidCommentDeleteException("Не вказано час створення коментаря (createdAt)");
    Instant now = clock.instant();
    if (createdAt.isAfter(now))
      throw new InvalidCommentDeleteException("Час створення коментаря не може бути в майбутньому");
    if (!createdAt.isAfter(now.minus(DELETE_WINDOW))) throw new CommentTooOldException();

    if (!repo.bookExists(bookId)) throw new BookNotFoundException(bookId);
    Comment stored = repo.findById(bookId, commentId)
        .orElseThrow(() -> new CommentNotFoundException(bookId, commentId));
    if (!stored.createdAt().truncatedTo(ChronoUnit.MILLIS)
        .equals(createdAt.truncatedTo(ChronoUnit.MILLIS)))
      throw new InvalidCommentDeleteException(
          "Час створення не збігається зі збереженим коментарем");
    if (!repo.delete(bookId, commentId)) throw new CommentNotFoundException(bookId, commentId);
  }

  private CatalogService catalog() {
    if (catalog == null) throw new IllegalStateException("CommentService створено без CatalogService");
    return catalog;
  }

  private UserService users() {
    if (users == null) throw new IllegalStateException("CommentService створено без UserService");
    return users;
  }
}
