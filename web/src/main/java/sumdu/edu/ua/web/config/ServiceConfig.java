package sumdu.edu.ua.web.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.service.CatalogService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.core.service.UserService;

/**
 * Lab 10: registers the core CommentService as a Spring bean. Core has no Spring annotations,
 * so the bean is declared here; the AOP aspects then wrap it into a proxy.
 */
@Configuration
public class ServiceConfig {

  @Bean
  public CommentService commentService(CommentRepositoryPort commentRepositoryPort,
      CatalogService catalogService, UserService userService, Clock applicationClock) {
    // delete(...) needs only the port; add/list (labs 5-9) also use the catalog and users.
    return new CommentService(commentRepositoryPort, catalogService, userService, applicationClock);
  }
}
