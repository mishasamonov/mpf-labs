package sumdu.edu.ua.web.config;

import java.util.Map;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import sumdu.edu.ua.web.security.ApiSecurityErrors;

/**
 * Two filter chains: stateless HTTP Basic for the REST API and form login for the pages.
 * Users come from AppUserDetailsService (users table), passwords are checked with BCrypt.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize on CatalogService, CommentService, UserService.
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** /api/** for Postman: no login page and no session, 401/403 as JSON. */
  @Bean
  @Order(1)
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ApiSecurityErrors errors)
      throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
            .anyRequest().hasRole("ADMIN"))
        .httpBasic(basic -> basic.realmName("Bookshelf API").authenticationEntryPoint(errors))
        .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable); // No session cookie, so no CSRF risk.
    return http.build();
  }

  /** Thymeleaf pages: the first matching rule wins, so specific rules go first. */
  @Bean
  @Order(2)
  public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**", "/webjars/**",
                "/favicon.ico").permitAll()
            .requestMatchers("/login", "/register", "/confirm").permitAll()
            .requestMatchers("/403", "/404", "/error", "/error-page").permitAll()
            // Complex lab: Render health check and info are public, metrics only for ADMIN.
            .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                .permitAll()
            .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
            .requestMatchers("/users", "/users/*/role").hasRole("ADMIN")
            .requestMatchers("/books/new", "/books/add", "/books/*/edit").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/comments/delete").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/comments/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/books/**", "/comments/**")
                .hasAnyRole("USER", "ADMIN")
            .requestMatchers("/books/**").hasRole("ADMIN") // POST create / edit / delete.
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .defaultSuccessUrl("/books", true)
            .failureHandler(loginFailureHandler())
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .deleteCookies("JSESSIONID")
            .permitAll())
        .exceptionHandling(e -> e.accessDeniedPage("/403"));
    return http.build();
  }

  /** Unconfirmed account (disabled) gets its own message on the login page. */
  private AuthenticationFailureHandler loginFailureHandler() {
    var handler = new ExceptionMappingAuthenticationFailureHandler();
    handler.setExceptionMappings(Map.of(DisabledException.class.getName(), "/login?unverified"));
    handler.setDefaultFailureUrl("/login?error");
    return handler;
  }

  /** Publishes AuthorizationDeniedEvent for the security audit log. */
  @Bean
  public AuthorizationEventPublisher authorizationEventPublisher(
      ApplicationEventPublisher publisher) {
    return new SpringAuthorizationEventPublisher(publisher);
  }
}
