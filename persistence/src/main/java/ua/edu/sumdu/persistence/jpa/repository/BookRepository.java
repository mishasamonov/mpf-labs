package ua.edu.sumdu.persistence.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.sumdu.persistence.jpa.entity.BookEntity;

public interface BookRepository extends JpaRepository<BookEntity, Long> {
  // Spring Data escapes % and _ in Containing parameters, so input is matched literally.
  Page<BookEntity> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
      String title, String author, Pageable pageable);
}
