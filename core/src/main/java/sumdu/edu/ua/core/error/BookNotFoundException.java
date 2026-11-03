package sumdu.edu.ua.core.error;

/** Lab 10: the book of the comment being deleted does not exist. */
public class BookNotFoundException extends RuntimeException {
  public BookNotFoundException(long bookId) {
    super("Книгу з id=" + bookId + " не знайдено");
  }
}
