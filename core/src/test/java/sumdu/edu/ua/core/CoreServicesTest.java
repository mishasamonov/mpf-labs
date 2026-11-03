package sumdu.edu.ua.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import sumdu.edu.ua.core.domain.*;
import sumdu.edu.ua.core.error.*;
import sumdu.edu.ua.core.dto.RegisterUserDto;
import sumdu.edu.ua.core.port.*;
import sumdu.edu.ua.core.security.AppUserDetailsService;
import sumdu.edu.ua.core.service.*;

/** Labs 5-9 behaviour of the core services over hand-written in-memory ports. */
class CoreServicesTest {
  final Instant now = Instant.parse("2026-10-29T12:00:00Z");
  final Map<Long, Book> bookData = new HashMap<>(Map.of(1L, new Book(1, "Java", "Блох", 2018)));
  final Map<Long, Comment> data = new HashMap<>();
  final Map<Long, User> userData =
      new HashMap<>(Map.of(
          7L, new User(7, "mykhailo@bookshelf.test", "$2a$04$hash", "mykhailo", "USER", true, null),
          8L, new User(8, "olena@bookshelf.test", "$2a$04$hash", "olena", "ADMIN", true, null)));
  final CatalogRepositoryPort books =
      new CatalogRepositoryPort() {
        public Optional<Book> findById(long id) {
          return Optional.ofNullable(bookData.get(id));
        }

        public List<Book> findAll() {
          return List.copyOf(bookData.values());
        }

        public Book save(Book b) {
          long id = b.id() == 0 ? 100 + bookData.size() : b.id();
          var saved = new Book(id, b.title(), b.author(), b.pubYear());
          bookData.put(id, saved);
          return saved;
        }

        public boolean deleteById(long id) {
          data.values().removeIf(c -> c.bookId() == id); // Same effect as the JPA cascade.
          return bookData.remove(id) != null;
        }

        public Page<Book> search(String q, PageRequest p) {
          return new Page<>(List.of(), p, 0);
        }
      };
  final CommentRepositoryPort comments =
      new CommentRepositoryPort() {
        public Optional<Comment> findById(long b, long id) {
          return Optional.ofNullable(data.get(id)).filter(c -> c.bookId() == b);
        }

        public boolean bookExists(long b) {
          return bookData.containsKey(b);
        }

        public Optional<Comment> update(long b, long id, String a, String t) {
          var old = findById(b, id);
          old.ifPresent(c -> data.put(id, new Comment(id, b, c.userId(), a, t, c.createdAt())));
          return findById(b, id);
        }

        public boolean delete(long b, long id) {
          return data.remove(id) != null;
        }

        public Comment add(long b, long u, String a, String t, Instant at) {
          long id = data.size() + 1;
          var c = new Comment(id, b, u, a, t, at);
          data.put(id, c);
          return c;
        }

        public Page<Comment> list(long b, String a, Instant s, PageRequest p) {
          return new Page<>(List.copyOf(data.values()), p, data.size());
        }

        public List<UserComment> findByUserId(long userId) {
          return data.values().stream()
              .filter(c -> Objects.equals(c.userId(), userId))
              .map(c -> new UserComment(c.id(), c.text(), c.createdAt(), bookData.get(c.bookId())))
              .toList();
        }
      };
  final UserRepositoryPort usersRepo =
      new UserRepositoryPort() {
        public List<User> findAll() {
          return List.copyOf(userData.values());
        }

        public Optional<User> findById(long id) {
          return Optional.ofNullable(userData.get(id));
        }

        public Optional<User> findByEmail(String email) {
          return userData.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        public Optional<User> findByVerificationToken(String token) {
          return userData.values().stream()
              .filter(u -> token.equals(u.getVerificationToken())).findFirst();
        }

        public User save(User u) {
          long id = u.getId() == 0 ? 100 + userData.size() : u.getId();
          var saved = new User(id, u.getEmail(), u.getPassword(), u.getNickname(), u.getRole(),
              u.isVerified(), u.getVerificationToken());
          userData.put(id, saved);
          return saved;
        }

        public boolean deleteById(long id) {
          return userData.remove(id) != null;
        }
      };
  final CatalogService catalog = new CatalogService(books);
  final List<String> sentTokens = new ArrayList<>();
  final MailPort mail = (email, token) -> sentTokens.add(email + " " + token);
  final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
  final UserService users = new UserService(usersRepo, encoder, mail);
  final CommentService service =
      new CommentService(comments, catalog, users, Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void authorNameIsTakenFromUserAccount() {
    var c = service.add(1, 7, "  Корисна книга  ");
    assertEquals(now, c.createdAt());
    assertEquals(7L, c.userId());
    assertEquals("mykhailo", c.author());
    assertEquals("Корисна книга", c.text());
  }

  @Test
  void commentsOfUserContainBookAndIgnoreOtherUsers() {
    service.add(1, 7, "Перший");
    service.add(1, 8, "Чужий");
    var list = service.getCommentsByUser(7);
    assertEquals(1, list.size());
    assertEquals("Перший", list.get(0).text());
    assertEquals("Java", list.get(0).book().title());
  }

  @Test
  void unknownUserIs404() {
    assertThrows(NotFoundException.class, () -> service.add(1, 999, "Text"));
    assertThrows(NotFoundException.class, () -> service.getCommentsByUser(999));
    assertTrue(data.isEmpty());
  }

  @Test
  void updatesTrimmedTextAndPreservesCreationTime() {
    var original = service.add(1, 7, "Old");
    var updated = service.update(1, original.id(), " B ", " New ");
    assertEquals("B", updated.author());
    assertEquals("New", updated.text());
    assertEquals(original.createdAt(), updated.createdAt());
  }

  @Test
  void rejectsBlankUpdateWithoutChangingData() {
    service.add(1, 7, "Old");
    assertThrows(ValidationException.class, () -> service.update(1, 1, "A", " "));
    assertEquals("Old", data.get(1L).text());
  }

  @Test
  void deleteRemovesStoredCommentWhenTimestampMatches() {
    var c = service.add(1, 7, "Text");
    service.delete(1, c.id(), c.createdAt());
    assertTrue(data.isEmpty());
  }

  @Test
  void rejectsBlankAndOversizedText() {
    assertThrows(ValidationException.class, () -> service.add(1, 7, " "));
    assertThrows(ValidationException.class, () -> service.add(1, 7, "t".repeat(1001)));
  }

  @Test
  void missingBookAndCommentAre404() {
    assertThrows(NotFoundException.class, () -> service.add(2, 7, "Text"));
    assertThrows(CommentNotFoundException.class, () -> service.delete(1, 999, now));
    assertThrows(BookNotFoundException.class, () -> service.delete(2, 1, now));
  }

  @Test
  void catalogCrudSavesUpdatesAndDeletesWithComments() {
    var saved = catalog.add("  Hibernate  ", " Gavin King ", 2006);
    assertEquals("Hibernate", saved.title());
    var updated = catalog.save(new Book(saved.id(), "Hibernate in Action", "Gavin King", 2004));
    assertEquals(saved.id(), updated.id());
    assertEquals(2004, catalog.findById(saved.id()).pubYear());
    service.add(saved.id(), 7, "Text");
    catalog.delete(saved.id());
    assertThrows(NotFoundException.class, () -> catalog.findById(saved.id()));
    assertTrue(data.isEmpty());
    assertThrows(NotFoundException.class, () -> catalog.save(new Book(999, "T", "A", 2000)));
  }

  @Test
  void userServiceValidatesRoleAndUniqueEmail() {
    assertThrows(ValidationException.class,
        () -> users.save(new User(0, "new@bookshelf.test", "$2a$x", "new", "ROOT", true, null)));
    assertThrows(ConflictException.class,
        () -> users.save(new User(0, " OLENA@bookshelf.test ", "$2a$x", "o", "USER", true, null)));
    var created = users.save(new User(0, " Reader@Bookshelf.test ", "$2a$x", "reader", "USER", true, null));
    assertEquals("reader@bookshelf.test", users.findByEmailOrThrow("READER@bookshelf.test").getEmail());
    users.delete(created.getId());
    assertThrows(NotFoundException.class, () -> users.findById(created.getId()));
    assertFalse(created.toString().contains("$2a$")); // Lombok @ToString excludes the hash.
  }

  RegisterUserDto dto(String email, String password, String nickname) {
    var dto = new RegisterUserDto();
    dto.setEmail(email);
    dto.setPassword(password);
    dto.setNickname(nickname);
    return dto;
  }

  @Test
  void registerHashesPasswordAndCreatesUnverifiedRegisteredAccount() {
    var saved = users.register(dto(" Taras@Bookshelf.TEST ", "secret77", " "));
    assertEquals("taras@bookshelf.test", saved.getEmail());
    assertEquals("taras", saved.getNickname()); // Default nickname: part before @.
    assertEquals(User.ROLE_REGISTERED, saved.getRole());
    assertFalse(saved.isVerified());
    assertNotEquals("secret77", saved.getPassword());
    assertTrue(saved.getPassword().startsWith("$2a$04$"));
    assertTrue(encoder.matches("secret77", saved.getPassword()));
    assertEquals(List.of("taras@bookshelf.test " + saved.getVerificationToken()), sentTokens);
  }

  @Test
  void registerRejectsDuplicateEmailAndShortPassword() {
    assertThrows(ConflictException.class, () -> users.register(dto("olena@bookshelf.test", "secret77", "o")));
    assertThrows(ValidationException.class, () -> users.register(dto("new@bookshelf.test", "12345", "n")));
    assertThrows(ValidationException.class, () -> users.register(dto("not-an-email", "secret77", "n")));
    assertTrue(sentTokens.isEmpty());
  }

  @Test
  void verifyAccountPromotesToUserAndTokenIsSingleUse() {
    var saved = users.register(dto("ivan@bookshelf.test", "secret77", "Іван"));
    String token = saved.getVerificationToken();
    assertTrue(users.verifyAccount(token));
    var verified = users.findByEmailOrThrow("ivan@bookshelf.test");
    assertTrue(verified.isVerified());
    assertEquals(User.ROLE_USER, verified.getRole());
    assertNull(verified.getVerificationToken());
    assertFalse(users.verifyAccount(token));
    assertFalse(users.verifyAccount("unknown"));
    assertFalse(users.verifyAccount(null));
  }

  @Test
  void userDetailsServiceMapsRoleAndDisablesUnverifiedAccounts() {
    var details = new AppUserDetailsService(users);
    var admin = details.loadUserByUsername("OLENA@bookshelf.test");
    assertEquals("olena@bookshelf.test", admin.getUsername());
    assertTrue(admin.isEnabled());
    assertEquals("ROLE_ADMIN", admin.getAuthorities().iterator().next().getAuthority());
    users.register(dto("new@bookshelf.test", "secret77", "new"));
    var registered = details.loadUserByUsername("new@bookshelf.test");
    assertFalse(registered.isEnabled());
    assertEquals("ROLE_REGISTERED", registered.getAuthorities().iterator().next().getAuthority());
    assertThrows(UsernameNotFoundException.class, () -> details.loadUserByUsername("nobody@x.test"));
  }

  @Test
  void changeRoleAcceptsOnlyKnownRoles() {
    assertEquals("ADMIN", users.changeRole(7, "ADMIN").getRole());
    assertThrows(ValidationException.class, () -> users.changeRole(7, "MODERATOR_X"));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 101})
  void rejectsInvalidSizes(int size) {
    assertThrows(ValidationException.class, () -> new PageRequest(0, size, "id,asc"));
  }

  @Test
  void rejectsInvalidPagingAndSort() {
    assertThrows(ValidationException.class, () -> new PageRequest(-1, 10, "id,asc"));
    assertThrows(ValidationException.class, () -> new PageRequest(0, 10, "id;DROP TABLE books"));
    assertThrows(
        ValidationException.class,
        () -> catalog.search("", new PageRequest(0, 10, "createdAt,asc")));
  }

  @Test
  void offsetDoesNotOverflow() {
    assertEquals(214748364700L, new PageRequest(Integer.MAX_VALUE, 100, "id,asc").offset());
  }
}
