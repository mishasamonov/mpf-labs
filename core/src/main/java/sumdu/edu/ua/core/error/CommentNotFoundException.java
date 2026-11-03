package sumdu.edu.ua.core.error;

/** Lab 10: the book exists, but it has no comment with this id. */
public class CommentNotFoundException extends RuntimeException {
  public CommentNotFoundException(long bookId, long commentId) {
    super("Коментар id=" + commentId + " у книзі id=" + bookId + " не знайдено");
  }
}
