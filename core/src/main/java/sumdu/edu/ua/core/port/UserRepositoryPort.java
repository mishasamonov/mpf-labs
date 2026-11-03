package sumdu.edu.ua.core.port;

import java.util.List;
import java.util.Optional;
import sumdu.edu.ua.core.domain.User;

public interface UserRepositoryPort {
  List<User> findAll();

  Optional<User> findById(long id);

  Optional<User> findByEmail(String email);

  Optional<User> findByVerificationToken(String token);

  /** id == 0 inserts a new user, otherwise updates the existing one. */
  User save(User user);

  boolean deleteById(long id);
}
