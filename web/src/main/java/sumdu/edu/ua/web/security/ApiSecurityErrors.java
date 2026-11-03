package sumdu.edu.ua.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** JSON answers of the API chain: 401 for guests, 403 for a role without rights. */
@Component
public class ApiSecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper json = new ObjectMapper();

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException e) throws IOException {
    response.setHeader("WWW-Authenticate", "Basic realm=\"Bookshelf API\"");
    write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
        "Потрібна автентифікація: HTTP Basic (email і пароль)");
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException e) throws IOException {
    write(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
        "Недостатньо прав: операція доступна лише ролі ADMIN");
  }

  private void write(HttpServletRequest request, HttpServletResponse response, int status,
      String error, String message) throws IOException {
    var body = new LinkedHashMap<String, Object>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status);
    body.put("error", error);
    body.put("message", message);
    body.put("path", request.getRequestURI());
    response.setStatus(status);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    json.writeValue(response.getOutputStream(), body);
  }
}
