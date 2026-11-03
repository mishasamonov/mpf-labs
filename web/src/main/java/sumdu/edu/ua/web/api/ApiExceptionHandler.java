package sumdu.edu.ua.web.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sumdu.edu.ua.core.error.*;

/** JSON errors of the API. AccessDeniedException is left to ApiSecurityErrors (403). */
@RestControllerAdvice(basePackages = "sumdu.edu.ua.web.api")
public class ApiExceptionHandler {
  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Map<String, Object>> missing(NotFoundException e) {
    return body(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
    return body(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler({ValidationException.class, HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class})
  ResponseEntity<Map<String, Object>> invalid(Exception e) {
    return body(HttpStatus.BAD_REQUEST,
        e instanceof ValidationException ? e.getMessage() : "Некоректний запит");
  }

  private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(Map.of("status", status.value(), "error", status.getReasonPhrase(),
            "message", message));
  }
}
