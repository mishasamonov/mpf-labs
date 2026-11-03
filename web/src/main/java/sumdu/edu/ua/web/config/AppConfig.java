package sumdu.edu.ua.web.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import sumdu.edu.ua.core.port.*;
import sumdu.edu.ua.core.security.AppUserDetailsService;
import sumdu.edu.ua.core.service.*;
import ua.edu.sumdu.persistence.jpa.PersistenceConfig;

/**
 * Wires core services to the JPA adapters of the persistence module. No JDBC here.
 * CommentService is registered by ServiceConfig (lab 10).
 */
@Configuration
@Import(PersistenceConfig.class)
public class AppConfig {
  @Bean
  public Clock applicationClock(@Value("${catalog.clock-zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }

  @Bean
  public CatalogService catalogService(CatalogRepositoryPort repository) {
    return new CatalogService(repository);
  }

  @Bean
  public UserService userService(
      UserRepositoryPort repository, PasswordEncoder passwordEncoder, MailPort mailPort) {
    return new UserService(repository, passwordEncoder, mailPort);
  }

  /** The only UserDetailsService: Boot no longer creates the default in-memory user. */
  @Bean
  public AppUserDetailsService appUserDetailsService(UserService userService) {
    return new AppUserDetailsService(userService);
  }
}
