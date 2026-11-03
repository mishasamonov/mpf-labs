package sumdu.edu.ua.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;
import sumdu.edu.ua.core.error.*;

/**
 * Lab 10 (extension): HTML errors of the page controllers. Instead of rendering an error view
 * in place, it redirects to /error-page and passes the status and the message as flash
 * attributes (Post/Redirect/Get). AccessDeniedException is not handled here on purpose:
 * it reaches Spring Security, which forwards to /403.
 */
@ControllerAdvice(basePackages = "sumdu.edu.ua.web.controller")
public class GlobalMvcExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalMvcExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public String missing(NotFoundException e, HttpServletRequest request) {
    return redirect(request, HttpStatus.NOT_FOUND, "error.missing", e.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public String conflict(ConflictException e, HttpServletRequest request) {
    return redirect(request, HttpStatus.CONFLICT, "error.conflict", e.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public String invalid(ValidationException e, HttpServletRequest request) {
    return redirect(request, HttpStatus.BAD_REQUEST, "error.invalid", e.getMessage());
  }

  @ExceptionHandler({MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class})
  public String badParameter(Exception e, HttpServletRequest request) {
    return redirect(request, HttpStatus.BAD_REQUEST, "error.invalid", null);
  }

  @ExceptionHandler(StorageException.class)
  public String storage(StorageException e, HttpServletRequest request) {
    log.error("Database request failed", e);
    return redirect(request, HttpStatus.INTERNAL_SERVER_ERROR, "error.server", null);
  }

  private static String redirect(HttpServletRequest request, HttpStatus status, String key,
      String detail) {
    log.info("{} {} -> {}, redirect to /error-page: {}", request.getMethod(),
        request.getRequestURI(), status.value(), detail);
    FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
    flash.put("status", status.value());
    flash.put("messageKey", key);
    flash.put("detail", detail);
    flash.put("path", request.getRequestURI());
    return "redirect:/error-page"; // RedirectView saves the flash map for this target.
  }
}
