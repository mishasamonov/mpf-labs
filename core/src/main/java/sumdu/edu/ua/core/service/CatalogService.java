package sumdu.edu.ua.core.service;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import sumdu.edu.ua.core.domain.*;
import sumdu.edu.ua.core.error.*;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;

/** Not final: the container wraps it into a transactional proxy. */
@Transactional
public class CatalogService {
  private final CatalogRepositoryPort repo;

  public CatalogService(CatalogRepositoryPort repo) {
    this.repo = repo;
  }

  public List<Book> findAll() {
    return repo.findAll();
  }

  public Book findById(long id) {
    return get(id);
  }

  public Book get(long id) {
    if (id <= 0) throw new ValidationException("id має бути додатним");
    return repo.findById(id).orElseThrow(() -> new NotFoundException("Книгу не знайдено"));
  }

  public Page<Book> search(String query, PageRequest request) {
    String q = query == null ? "" : query.trim();
    if (q.length() > 255) throw new ValidationException("q має містити не більше 255 символів");
    if (request.sort().startsWith("createdAt"))
      throw new ValidationException("Для книг не підтримується сортування createdAt");
    return repo.search(q, request);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Book add(String title, String author, int year) {
    return save(new Book(0, title, author, year));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Book save(Book book) {
    if (book.id() < 0) throw new ValidationException("id не може бути від'ємним");
    if (book.id() > 0) get(book.id());
    String title = clean(book.title(), 255, "Назва");
    String author = clean(book.author(), 255, "Автор");
    if (book.pubYear() < 1 || book.pubYear() > 9999)
      throw new ValidationException("Некоректний рік видання");
    return repo.save(new Book(book.id(), title, author, book.pubYear()));
  }

  /** Comments of the book are removed by cascade. */
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(long id) {
    get(id);
    if (!repo.deleteById(id)) throw new NotFoundException("Книгу вже видалено");
  }

  public static String clean(String value, int max, String field) {
    if (value == null || value.isBlank() || value.trim().length() > max)
      throw new ValidationException(field + ": обов'язкове значення довжиною до " + max);
    return value.trim();
  }
}
