package sumdu.edu.ua.core.error;

/**
 * Lab 10 (extension): the comment text is blank, too long or contains a forbidden word.
 * It is a ValidationException, so the existing handlers answer 400 for it.
 */
public class InvalidCommentTextException extends ValidationException {
  public InvalidCommentTextException(String message) {
    super(message);
  }
}
