package sumdu.edu.ua.core.error;

/** Lab 10: a comment may be deleted only during the first 24 hours after it was created. */
public class CommentTooOldException extends RuntimeException {
  public static final String MESSAGE =
      "Коментар створено більше ніж 24 години тому і не може бути видалений";

  public CommentTooOldException() {
    super(MESSAGE);
  }
}
