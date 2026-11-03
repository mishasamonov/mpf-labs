package ua.edu.sumdu.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ua.edu.sumdu.persistence.jpa.entity.CommentEntity;

public interface CommentRepository
    extends JpaRepository<CommentEntity, Long>, JpaSpecificationExecutor<CommentEntity> {
  // One JOIN query instead of a separate SELECT per lazy book (N+1).
  @EntityGraph(attributePaths = "book")
  List<CommentEntity> findByUserId(Long userId);

  Optional<CommentEntity> findByIdAndBookId(Long id, Long bookId);
}
