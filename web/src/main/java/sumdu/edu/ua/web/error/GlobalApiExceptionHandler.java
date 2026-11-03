package sumdu.edu.ua.web.error;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sumdu.edu.ua.core.error.*;

/**
 * Lab 10: turns the exceptions of CommentService.delete into JSON {"error": "..."}.
 * The highest order puts it before the HTML advice of the page controllers.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

  @ExceptionHandler(InvalidCommentDeleteException.class)
  public ResponseEntity<Map<String, String>> invalidDelete(InvalidCommentDeleteException e) {
    log.warn("Invalid comment delete request: {}", e.getMessage());
    return error(HttpStatus.BAD_REQUEST, e);
  }

  @ExceptionHandler(CommentTooOldException.class)
  public ResponseEntity<Map<String, String>> tooOld(CommentTooOldException e) {
    log.warn("Comment delete refused, older than 24 hours: {}", e.getMessage());
    return error(HttpStatus.BAD_REQUEST, e);
  }

  @ExceptionHandler(BookNotFoundException.class)
  public ResponseEntity<Map<String, String>> bookNotFound(BookNotFoundException e) {
    log.info("Book not found: {}", e.getMessage());
    return error(HttpStatus.NOT_FOUND, e);
  }

  @ExceptionHandler(CommentNotFoundException.class)
  public ResponseEntity<Map<String, String>> commentNotFound(CommentNotFoundException e) {
    log.info("Comment not found: {}", e.getMessage());
    return error(HttpStatus.NOT_FOUND, e);
  }

  private static ResponseEntity<Map<String, String>> error(HttpStatus status, Exception e) {
    return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
  }
}
