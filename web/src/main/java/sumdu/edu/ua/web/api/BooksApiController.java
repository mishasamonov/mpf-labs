package sumdu.edu.ua.web.api;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sumdu.edu.ua.web.service.CatalogFacade;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.error.ValidationException;
import sumdu.edu.ua.core.service.CatalogService;

/**
 * JSON API of the catalog for Postman. Guests get 401, USER may only read (GET),
 * changes require ADMIN: SecurityConfig.apiSecurityFilterChain + @PreAuthorize in CatalogService.
 */
@RestController
@RequestMapping("/api/books")
public class BooksApiController {
  private final CatalogService catalog;

  public BooksApiController(CatalogFacade services) {
    this.catalog = services.catalog();
  }

  public record BookRequest(String title, String author, Integer pubYear) {
    int year() {
      if (pubYear == null) throw new ValidationException("pubYear обов'язковий");
      return pubYear;
    }
  }

  @GetMapping
  public List<Book> list(@RequestParam(required = false) String q) {
    return catalog.search(q, new PageRequest(0, 100, "id,asc")).items();
  }

  @GetMapping("/{id}")
  public Book get(@PathVariable long id) {
    return catalog.get(id);
  }

  @PostMapping
  public ResponseEntity<Book> create(@RequestBody BookRequest request) {
    Book saved = catalog.add(request.title(), request.author(), request.year());
    return ResponseEntity.created(URI.create("/api/books/" + saved.id())).body(saved);
  }

  @PutMapping("/{id}")
  public Book update(@PathVariable long id, @RequestBody BookRequest request) {
    return catalog.save(new Book(id, request.title(), request.author(), request.year()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    catalog.delete(id);
    return ResponseEntity.noContent().build();
  }
}
