package ua.edu.sumdu.persistence.jpa.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.*;
import sumdu.edu.ua.core.error.NotFoundException;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import ua.edu.sumdu.persistence.jpa.entity.BookEntity;
import ua.edu.sumdu.persistence.jpa.repository.BookRepository;

@Component
@Transactional(readOnly = true)
public class JpaCatalogRepositoryAdapter implements CatalogRepositoryPort {
  private final BookRepository repo;

  public JpaCatalogRepositoryAdapter(BookRepository repo) {
    this.repo = repo;
  }

  @Override
  public Page<Book> search(String query, PageRequest request) {
    var pageable = EntityMapper.pageable(request, EntityMapper.sort(request.sort(), Sort.Direction.ASC));
    var page = repo.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query, pageable);
    return new Page<>(page.map(EntityMapper::book).getContent(), request, page.getTotalElements());
  }

  @Override
  public List<Book> findAll() {
    return repo.findAll(Sort.by("id")).stream().map(EntityMapper::book).toList();
  }

  @Override
  public Optional<Book> findById(long id) {
    return repo.findById(id).map(EntityMapper::book);
  }

  @Override
  @Transactional
  public Book save(Book book) {
    BookEntity entity = book.id() == 0
        ? new BookEntity(book.title(), book.author(), book.pubYear())
        : repo.findById(book.id()).orElseThrow(() -> new NotFoundException("Книгу не знайдено"));
    entity.setTitle(book.title());
    entity.setAuthor(book.author());
    entity.setYear(book.pubYear());
    return EntityMapper.book(repo.save(entity)); // Dirty checking issues UPDATE for existing rows.
  }

  @Override
  @Transactional
  public boolean deleteById(long id) {
    return repo.findById(id).map(entity -> {
      repo.delete(entity); // CascadeType.ALL removes the comments first.
      return true;
    }).orElse(false);
  }
}
