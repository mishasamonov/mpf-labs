package sumdu.edu.ua.core.error;

/** Lab 10: invalid arguments of CommentService.delete (ids, missing or impossible createdAt). */
public class InvalidCommentDeleteException extends RuntimeException {
  public InvalidCommentDeleteException(String message) {
    super(message);
  }
}
