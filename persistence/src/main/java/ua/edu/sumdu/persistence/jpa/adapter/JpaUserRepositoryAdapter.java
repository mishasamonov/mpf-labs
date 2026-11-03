package ua.edu.sumdu.persistence.jpa.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.error.NotFoundException;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import ua.edu.sumdu.persistence.jpa.entity.UserEntity;
import ua.edu.sumdu.persistence.jpa.repository.UserRepository;

@Component
@Transactional(readOnly = true)
public class JpaUserRepositoryAdapter implements UserRepositoryPort {
  private final UserRepository repo;

  public JpaUserRepositoryAdapter(UserRepository repo) {
    this.repo = repo;
  }

  @Override
  public List<User> findAll() {
    return repo.findAll(Sort.by("id")).stream().map(EntityMapper::user).toList();
  }

  @Override
  public Optional<User> findById(long id) {
    return repo.findById(id).map(EntityMapper::user);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return repo.findByEmail(email).map(EntityMapper::user);
  }

  @Override
  public Optional<User> findByVerificationToken(String token) {
    return repo.findByVerificationToken(token).map(EntityMapper::user);
  }

  @Override
  @Transactional
  public User save(User user) {
    UserEntity entity = user.getId() == 0
        ? new UserEntity(user.getEmail(), user.getPassword(), user.getNickname(), user.getRole())
        : repo.findById(user.getId())
            .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
    entity.setEmail(user.getEmail());
    entity.setPassword(user.getPassword());
    entity.setNickname(user.getNickname());
    entity.setRole(user.getRole());
    entity.setVerified(user.isVerified());
    entity.setVerificationToken(user.getVerificationToken());
    return EntityMapper.user(repo.save(entity));
  }

  @Override
  @Transactional
  public boolean deleteById(long id) {
    return repo.findById(id).map(entity -> {
      repo.delete(entity); // @PreRemove + FK ON DELETE SET NULL keep the comments.
      return true;
    }).orElse(false);
  }
}
