package ua.edu.sumdu.persistence.jpa.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.sumdu.persistence.jpa.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByEmail(String email);

  Optional<UserEntity> findByVerificationToken(String verificationToken);
}
