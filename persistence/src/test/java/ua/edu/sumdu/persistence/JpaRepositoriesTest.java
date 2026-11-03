package ua.edu.sumdu.persistence;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.*;
import ua.edu.sumdu.persistence.jpa.adapter.*;
import ua.edu.sumdu.persistence.jpa.repository.*;

@SpringBootTest(classes = JpaTestApplication.class,
    properties = {"spring.datasource.url=jdbc:h2:mem:lab10persistence;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=none"})
@Transactional // Every test is rolled back.
class JpaRepositoriesTest {
  @Autowired Flyway flyway;
  @Autowired EntityManager em;
  @Autowired BookRepository bookRepository;
  @Autowired UserRepository userRepository;
  @Autowired CommentRepository commentRepository;
  @Autowired JpaCatalogRepositoryAdapter catalog;
  @Autowired JpaCommentRepositoryAdapter comments;
  @Autowired JpaUserRepositoryAdapter users;

  @Test
  void flywayAppliedSchemaAndTestData() {
    var applied = flyway.info().applied();
    assertEquals(5, applied.length);
    assertEquals("init", applied[0].getDescription());
    assertEquals("test data", applied[1].getDescription());
    assertEquals("signup", applied[2].getDescription());
    assertEquals("cleanup users legacy", applied[3].getDescription());
    assertEquals("promote role", applied[4].getDescription());
    assertEquals(7, bookRepository.count());
    assertEquals(4, userRepository.count());
    assertEquals(7, commentRepository.count());
  }

  @Test
  void findByUserIdLoadsBooksInTheSameQuery() {
    var list = commentRepository.findByUserId(2L);
    assertEquals(3, list.size());
    assertTrue(list.stream().allMatch(c -> Hibernate.isInitialized(c.getBook())));
    var view = comments.findByUserId(2);
    assertEquals("Каталог рефакторингів зручно тримати під рукою.", view.get(0).text());
    assertEquals("Рефакторинг", view.get(0).book().title()); // Newest first.
  }

  @Test
  void relationsAreBidirectional() {
    var book = bookRepository.findById(7L).orElseThrow();
    assertEquals(2, book.getComments().size());
    var user = userRepository.findByEmail("olena@bookshelf.test").orElseThrow();
    assertTrue(user.getComments().stream().allMatch(c -> c.getUser() == user));
  }

  @Test
  void searchPaginatesAndMatchesWildcardsLiterally() {
    var first = catalog.search("мартін", new PageRequest(0, 1, "title,asc"));
    assertEquals(3, first.total());
    var second = catalog.search("мартін", new PageRequest(1, 1, "title,asc"));
    assertNotEquals(first.items().get(0).id(), second.items().get(0).id());
    assertEquals(2018, catalog.search("", new PageRequest(0, 10, "pubYear,desc")).items().get(0).pubYear());
    assertEquals(0, catalog.search("%", new PageRequest(0, 10, "id,asc")).total());
    assertEquals(0, catalog.search("' OR 1=1 --", new PageRequest(0, 10, "id,asc")).total());
  }

  @Test
  void saveInsertsWithNewIdentityAndUpdatesExistingRow() {
    var saved = catalog.save(new Book(0, "Spring Data JPA", "Петро", 2024));
    assertTrue(saved.id() >= 100);
    catalog.save(new Book(saved.id(), "Spring Data JPA, 2nd", "Петро", 2025));
    em.flush();
    em.clear();
    assertEquals(2025, bookRepository.findById(saved.id()).orElseThrow().getYear());
  }

  @Test
  void addCommentStoresUserAndInstantAndFilters() {
    Instant at = Instant.parse("2026-11-02T10:00:00.123456Z");
    var added = comments.add(1, 3, "olena", "Новий відгук", at);
    em.flush();
    em.clear();
    assertEquals(3L, added.userId());
    var page = comments.list(1, "OLE", at.minusSeconds(1), new PageRequest(0, 10, "createdAt,desc"));
    assertEquals(1, page.total());
    assertEquals(at, page.items().get(0).createdAt());
    assertEquals(0, comments.list(1, "%", null, new PageRequest(0, 10, "id,asc")).total());
  }

  @Test
  void deleteIsScopedToBook() {
    var c = comments.add(1, 2, "mykhailo", "Text", Instant.now());
    assertFalse(comments.delete(2, c.id()));
    assertTrue(comments.findById(1, c.id()).isPresent());
    assertTrue(comments.delete(1, c.id()));
    assertFalse(comments.delete(1, c.id()));
  }

  @Test
  void deletingBookCascadesToComments() {
    assertTrue(catalog.deleteById(7));
    em.flush();
    em.clear();
    assertTrue(bookRepository.findById(7L).isEmpty());
    assertEquals(5, commentRepository.count());
  }

  @Test
  void deletingUserKeepsCommentsWithoutAccount() {
    assertTrue(users.deleteById(2));
    em.flush();
    em.clear();
    assertEquals(7, commentRepository.count());
    assertTrue(commentRepository.findByUserId(2L).isEmpty());
    var orphan = commentRepository.findAll().stream()
        .filter(c -> c.getAuthor().equals("mykhailo")).findFirst().orElseThrow();
    assertNull(orphan.getUser());
  }

  @Test
  void emailIsUnique() {
    assertThrows(Exception.class, () -> {
      // IDENTITY: INSERT runs at once.
      users.save(new User(0, "admin@bookshelf.test", "$2a$10$x", "a", "USER", true, null));
      em.flush();
    });
  }

  @Test
  void signupMigrationsMovedLoginsToEmailAndPromotedAdmin() {
    var admin = users.findByEmail("mykhailo@bookshelf.test").orElseThrow();
    assertEquals("ADMIN", admin.getRole()); // V5__promote_role.
    assertEquals("mykhailo", admin.getNickname());
    assertTrue(admin.isVerified());
    assertTrue(admin.getPassword().startsWith("$2a$10$")); // {bcrypt} prefix removed by V3.
    assertEquals("USER", users.findByEmail("olena@bookshelf.test").orElseThrow().getRole());
    var columns = em.createNativeQuery(
        "select column_name from information_schema.columns where table_name = 'USERS'")
        .getResultList();
    assertTrue(columns.contains("EMAIL") && columns.contains("VERIFICATION_TOKEN"));
    assertFalse(columns.contains("USERNAME")); // V4__cleanup_users_legacy.
  }

  @Test
  void registeredAccountIsFoundByVerificationToken() {
    var saved = users.save(new User(0, "new@bookshelf.test", "$2a$10$hash", "new",
        "REGISTERED", false, "token-123"));
    em.flush();
    em.clear();
    var found = users.findByVerificationToken("token-123").orElseThrow();
    assertEquals(saved.getId(), found.getId());
    assertFalse(found.isVerified());
    assertEquals("REGISTERED", found.getRole());
    assertThrows(Exception.class, () -> {
      em.createNativeQuery("update users set role = 'ROOT' where id = " + saved.getId())
          .executeUpdate(); // CHECK ck_users_role.
    });
  }
}
