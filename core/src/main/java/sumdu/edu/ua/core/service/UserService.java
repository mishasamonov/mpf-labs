package sumdu.edu.ua.core.service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.dto.RegisterUserDto;
import sumdu.edu.ua.core.error.*;
import sumdu.edu.ua.core.port.MailPort;
import sumdu.edu.ua.core.port.UserRepositoryPort;

@Transactional
public class UserService {
  private static final Set<String> ROLES =
      Set.of(User.ROLE_REGISTERED, User.ROLE_USER, User.ROLE_ADMIN);
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private final UserRepositoryPort users;
  private final PasswordEncoder passwordEncoder;
  private final MailPort mailPort;

  public UserService(UserRepositoryPort users, PasswordEncoder passwordEncoder, MailPort mailPort) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.mailPort = mailPort;
  }

  @PreAuthorize("hasRole('ADMIN')")
  public List<User> findAll() {
    return users.findAll();
  }

  public User findById(long id) {
    if (id <= 0) throw new ValidationException("id має бути додатним");
    return users.findById(id).orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
  }

  /** Used by AppUserDetailsService during login, so it is open for anonymous callers. */
  public User findByEmailOrThrow(String email) {
    return users.findByEmail(normalizeEmail(email))
        .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
  }

  /** New account: BCrypt hash, role REGISTERED, unverified, one-time activation token. */
  public User register(RegisterUserDto dto) {
    String email = normalizeEmail(dto.getEmail());
    if (!EMAIL.matcher(email).matches()) throw new ValidationException("Некоректний email");
    if (dto.getPassword() == null || dto.getPassword().length() < 6
        || dto.getPassword().length() > 72)
      throw new ValidationException("Пароль має бути не менше 6 символів");
    if (users.findByEmail(email).isPresent())
      throw new ConflictException("Користувач з таким email вже існує");
    User u = new User();
    u.setEmail(email);
    u.setPassword(passwordEncoder.encode(dto.getPassword()));
    u.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
        ? email.substring(0, email.indexOf('@'))
        : CatalogService.clean(dto.getNickname(), 128, "Нікнейм"));
    u.setRole(User.ROLE_REGISTERED);
    u.setVerified(false);
    String token = UUID.randomUUID().toString();
    u.setVerificationToken(token);
    User saved = users.save(u);
    // Inside the transaction: if the letter cannot be sent the account is rolled back.
    mailPort.sendVerificationEmail(saved.getEmail(), token);
    return saved;
  }

  /** Activation link: the account becomes verified and is promoted to USER. */
  public boolean verifyAccount(String token) {
    if (token == null || token.isBlank()) return false;
    Optional<User> opt = users.findByVerificationToken(token.trim());
    if (opt.isEmpty()) return false;
    User u = opt.get();
    u.setVerified(true);
    if (User.ROLE_REGISTERED.equals(u.getRole())) u.setRole(User.ROLE_USER);
    u.setVerificationToken(null);
    users.save(u);
    return true;
  }

  @PreAuthorize("hasRole('ADMIN')")
  public User changeRole(long id, String role) {
    if (!ROLES.contains(role)) throw new ValidationException("Некоректна роль");
    User u = findById(id);
    u.setRole(role);
    return users.save(u);
  }

  /** The password must already be a hash; plain text is never stored. */
  @PreAuthorize("hasRole('ADMIN')")
  public User save(User user) {
    String email = normalizeEmail(user.getEmail());
    if (!EMAIL.matcher(email).matches()) throw new ValidationException("Некоректний email");
    CatalogService.clean(user.getPassword(), 255, "Пароль");
    if (!ROLES.contains(user.getRole())) throw new ValidationException("Некоректна роль");
    if (user.getId() > 0) findById(user.getId());
    if (users.findByEmail(email).filter(other -> other.getId() != user.getId()).isPresent())
      throw new ConflictException("Користувач з таким email вже існує");
    user.setEmail(email);
    return users.save(user);
  }

  /** Comments stay in the catalog: user_id becomes NULL. */
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(long id) {
    findById(id);
    if (!users.deleteById(id)) throw new NotFoundException("Користувача вже видалено");
  }

  static String normalizeEmail(String email) {
    return CatalogService.clean(email, 128, "Email").toLowerCase(Locale.ROOT);
  }
}
