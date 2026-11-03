package sumdu.edu.ua.core.port;

import java.util.List;
import java.util.Optional;
import sumdu.edu.ua.core.domain.*;

public interface CatalogRepositoryPort {
  Page<Book> search(String query, PageRequest request);

  List<Book> findAll();

  Optional<Book> findById(long id);

  /** id == 0 inserts a new book, otherwise updates the existing one. */
  Book save(Book book);

  boolean deleteById(long id);
}
