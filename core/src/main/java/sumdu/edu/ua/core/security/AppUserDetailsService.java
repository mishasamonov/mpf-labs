package sumdu.edu.ua.core.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.UserService;

/** Loads accounts from the users table for Spring Security; the login is the e-mail. */
public class AppUserDetailsService implements UserDetailsService {
  private final UserService userService;

  public AppUserDetailsService(UserService userService) {
    this.userService = userService;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User u;
    try {
      u = userService.findByEmailOrThrow(email);
    } catch (RuntimeException ex) {
      throw new UsernameNotFoundException("Користувача не знайдено");
    }
    String role = (u.getRole() == null || u.getRole().isBlank()) ? User.ROLE_USER : u.getRole();
    return org.springframework.security.core.userdetails.User
        .withUsername(u.getEmail())
        .password(u.getPassword())
        .roles(role) // ROLE_ prefix is added: hasRole('ADMIN') checks ROLE_ADMIN.
        .disabled(!u.isVerified()) // Until the e-mail is confirmed: DisabledException.
        .build();
  }
}
