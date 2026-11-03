package sumdu.edu.ua.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sumdu.edu.ua.core.error.NotFoundException;
import sumdu.edu.ua.core.service.UserService;

/** ${currentUserNickname}, ${currentTime} and ${uri} are available in every page template. */
@ControllerAdvice(basePackages = "sumdu.edu.ua.web.controller")
public class GlobalModelAttributes {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  private final UserService userService;
  private final Clock clock;

  public GlobalModelAttributes(UserService userService, Clock clock) {
    this.userService = userService;
    this.clock = clock;
  }

  @ModelAttribute("currentUserNickname")
  public String currentUserNickname() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()
        || auth instanceof AnonymousAuthenticationToken) {
      return null;
    }
    try {
      return userService.findByEmailOrThrow(auth.getName()).getNickname();
    } catch (NotFoundException e) {
      return auth.getName(); // Account deleted while the session is still alive.
    }
  }

  @ModelAttribute("currentTime")
  public String currentTime() {
    return LocalDateTime.now(clock).format(TIME);
  }

  @ModelAttribute("uri")
  public String uri(HttpServletRequest request) {
    String uri = request.getRequestURI().substring(request.getContextPath().length());
    return uri.isEmpty() ? "/" : uri;
  }
}
