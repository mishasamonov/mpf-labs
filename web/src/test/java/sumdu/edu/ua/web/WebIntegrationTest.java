package sumdu.edu.ua.web;

import static org.junit.jupiter.api.Assertions.*;
import static sumdu.edu.ua.web.TestBrowser.*;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;
import sumdu.edu.ua.web.service.CatalogFacade;
import sumdu.edu.ua.web.service.MailService;
import sumdu.edu.ua.core.domain.PageRequest;

/** Pages of labs 6-8 behind Spring Security: the same behaviour for a signed-in account. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:lab10tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "catalog.clock-zone=UTC"})
@WithMockUser(roles = "ADMIN") // Test-thread setup calls services guarded by @PreAuthorize.
class WebIntegrationTest {
  @MockBean MailService mailService;
  @LocalServerPort int port;
  @Autowired CatalogFacade services;
  @Autowired EntityManager em;
  @Autowired TransactionTemplate tx;

  TestBrowser admin() throws Exception { return TestBrowser.login(port, ADMIN, ADMIN_PASSWORD); }
  TestBrowser user() throws Exception { return TestBrowser.login(port, USER, USER_PASSWORD); }
  long total() { return services.catalog().search(null, new PageRequest(0, 10, "id,asc")).total(); }

  @Test void catalogRendersModelAsHtmlWithSharedHeader() throws Exception {
    var response = user().get("/books");
    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("text/html"));
    assertTrue(response.body().contains("Каталог книг"));
    assertTrue(response.body().contains("Чистий код"));
    assertTrue(response.body().contains("Книжкова полиця"));
    assertFalse(response.body().contains("th:each"));
    assertFalse(response.body().contains("sec:authorize"));
    assertFalse(response.body().contains("??"));
  }

  @Test void emptySearchRendersConditionalMessageAndHidesTable() throws Exception {
    var response = user().get("/books?q=no-such-book-782bf9");
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Немає книг"));
    assertFalse(response.body().contains("<table>"));
  }

  @Test void addFormAndMethodicalAliasUseBoundBookFields() throws Exception {
    var admin = admin();
    for (String path : new String[]{"/books/add", "/books/new"}) {
      var r = admin.get(path);
      assertEquals(200, r.statusCode());
      for (String field : new String[]{"title", "author", "pubYear", "_csrf"})
        assertTrue(r.body().contains("name=\"" + field + "\""));
      assertTrue(r.body().contains("action=\"/books\""));
    }
  }

  @Test void constructorBindingCreatesBookAndRedirectsWithoutTrustingId() throws Exception {
    var admin = admin();
    var response = admin.post("/books", Map.of("id", "999999", "title", " Thymeleaf інтеграція ",
        "author", " Самонов Михайло ", "pubYear", "2026"));
    assertEquals(302, response.statusCode());
    assertEquals("/books", URI.create(location(response)).getPath());
    var books = services.catalog().search("Thymeleaf інтеграція", new PageRequest(0, 10, "id,asc"));
    assertEquals(1, books.total());
    var saved = books.items().get(0);
    assertNotEquals(999999, saved.id());
    assertEquals("Thymeleaf інтеграція", saved.title());
    assertEquals("Самонов Михайло", saved.author());
    assertTrue(admin.get("/books?q=" + encode(saved.title())).body().contains(saved.title()));
  }

  @ParameterizedTest
  @CsvSource({"'',Author,2026", "Title,'',2026", "Title,Author,0", "Title,Author,10000"})
  void invalidFormKeepsValuesAndDoesNotInsert(String title, String author, String year) throws Exception {
    long before = total();
    var response = admin().post("/books", Map.of("id", "0", "title", title, "author", author, "pubYear", year));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("class=\"error\""));
    assertEquals(before, total());
  }

  @Test void nonnumericYearHasLocalizedBindingErrorAndKeepsText() throws Exception {
    long before = total();
    var response = admin().post("/books?lang=en", Map.of("id", "0", "title", "Keep this title",
        "author", "Keep this author", "pubYear", "abc"));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Enter a whole number"));
    assertTrue(response.body().contains("value=\"Keep this title\""));
    assertTrue(response.body().contains("value=\"Keep this author\""));
    assertEquals(before, total());
  }

  @ParameterizedTest
  @CsvSource({"en-US,Books catalog", "fr-FR, Catalogue des livres", "uk-UA,Каталог книг", "de-DE,Каталог книг"})
  void acceptLanguageNegotiatesSupportedLanguagesOrUkrainian(String language, String text) throws Exception {
    var response = user().get("/books", "Accept-Language", language);
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains(text));
  }

  @Test void parameterStoresThirtyDayCookieAndCookieOverridesHeader() throws Exception {
    var admin = admin();
    var response = admin.get("/books?lang=fr", "Accept-Language", "en");
    assertTrue(response.body().contains("Catalogue des livres"));
    String cookie = response.headers().allValues("Set-Cookie").stream()
        .filter(c -> c.startsWith("lang=")).findFirst().orElseThrow();
    assertTrue(cookie.contains("lang=fr"));
    assertTrue(cookie.contains("Max-Age=2592000"));
    var form = admin.get("/books/add", "Accept-Language", "en"); // Cookie jar sends lang=fr.
    assertTrue(form.body().contains("Nouveau livre"));
  }

  @Test void malformedLanguageHeaderFallsBackWithoutServerError() throws Exception {
    var response = new TestBrowser(port).get("/login", "Accept-Language", "this is invalid %%");
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Вхід до каталогу"));
  }

  @Test void commentAuthorIsTheSignedInUserAndOnlyAdminDeletes() throws Exception {
    long bookId = services.catalog().add("Comments lifecycle", "Tester", 2026).id();
    var user = user();
    assertTrue(user.get("/comments?bookId=" + bookId).body().contains("Відгуків ще немає"));
    var added = user.post("/comments?bookId=" + bookId,
        Map.of("userId", "1", "text", " Перевірено Spring Security ")); // userId is ignored.
    assertEquals(302, added.statusCode());
    var comments = services.comments().list(bookId, null, null, new PageRequest(0, 10, "createdAt,desc"));
    assertEquals(1, comments.total());
    var saved = comments.items().get(0);
    assertEquals("olena", saved.author()); // Nickname of the principal.
    assertEquals(3L, saved.userId());
    var html = user.get("/comments?bookId=" + bookId).body();
    assertTrue(html.contains("href=\"/users/3/comments\""));
    assertTrue(html.contains("Перевірено Spring Security"));
    assertFalse(html.contains("action=\"/comments/delete\"")); // No delete button for USER.
    var params = Map.of("bookId", "" + bookId, "commentId", "" + saved.id(),
        "createdAt", saved.createdAt().toString());
    var forbidden = user.post("/comments/delete", params);
    assertEquals(403, forbidden.statusCode());
    var admin = admin();
    assertTrue(admin.get("/comments?bookId=" + bookId).body().contains("action=\"/comments/delete\""));
    assertTrue(admin.get("/comments?bookId=" + bookId).body().contains("name=\"createdAt\""));
    var deleted = admin.post("/comments/delete", params);
    assertEquals(302, deleted.statusCode());
    assertEquals(0, services.comments().list(bookId, null, null, new PageRequest(0, 10, "createdAt,desc")).total());
  }

  @Test void invalidCommentIsRedisplayedWithoutSaving() throws Exception {
    long bookId = services.catalog().add("Invalid comments", "Tester", 2026).id();
    var response = user().post("/comments?bookId=" + bookId, Map.of("text", " "));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Введіть непорожнє значення"));
    assertEquals(0, services.comments().list(bookId, null, null, new PageRequest(0, 10, "createdAt,desc")).total());
  }

  @Test void forbiddenWordIsShownAsFormErrorFromCoreService() throws Exception {
    long bookId = services.catalog().add("Forbidden words", "Tester", 2026).id();
    var response = user().post("/comments?bookId=" + bookId, Map.of("text", "Найкраще казино тут"));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Коментар містить заборонене слово «казино»"));
    assertEquals(0, services.comments().list(bookId, null, null, new PageRequest(0, 10, "createdAt,desc")).total());
  }

  @Test void deletionAfterTwentyFourHoursIsJson400FromGlobalApiHandler() throws Exception {
    var saved = services.comments().add(1, 3, "Old review");
    tx.executeWithoutResult(status -> em.createQuery(
            "update CommentEntity c set c.createdAt = :at where c.id = :id")
        .setParameter("at", Instant.now().minusSeconds(90000))
        .setParameter("id", saved.id()).executeUpdate()); // JPQL, no JDBC.
    var old = services.comments().get(1, saved.id()).createdAt();
    var response = admin().post("/comments/delete", Map.of("bookId", "1", "commentId", "" + saved.id(),
        "createdAt", old.toString()));
    assertEquals(400, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
    assertEquals("{\"error\":\"Коментар створено більше ніж 24 години тому і не може бути видалений\"}",
        response.body());
    assertNotNull(services.comments().get(1, saved.id()));
  }

  @Test void missingBookReturnsLocalizedHtml404() throws Exception {
    var user = user();
    var redirect = user.get("/comments?bookId=999999&lang=en");
    assertEquals("/error-page", URI.create(location(redirect)).getPath());
    var response = user.follow(redirect);
    assertEquals(404, response.statusCode());
    assertTrue(response.body().contains("Книгу не знайдено")); // Detail from the core exception.
    assertTrue(response.body().contains("Book or review not found"));
  }

  @Test void modelTextIsEscapedInsteadOfRunningHtml() throws Exception {
    services.catalog().add("<script>alert(1)</script>", "Escape test", 2026);
    var response = user().get("/books?q=Escape+test");
    assertTrue(response.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    assertFalse(response.body().contains("<script>"));
  }

  @Test void paginationLinksAndInvalidPageAreHandled() throws Exception {
    for (int i = 0; i < 12; i++) services.catalog().add("Paging Book " + i, "Paging", 2026);
    var user = user();
    assertTrue(user.get("/books?q=Paging&page=0").body().contains("page=1"));
    assertTrue(user.get("/books?q=Paging&page=1").body().contains("Сторінка 2 з 2"));
    assertEquals(400, user.follow(user.get("/books?page=-1")).statusCode());
  }

  @Test void userCommentsPageListsCommentsWithBookLinks() throws Exception {
    var response = user().get("/users/2/comments");
    assertEquals(200, response.statusCode());
    var html = response.body();
    assertTrue(html.contains("Коментарі користувача: mykhailo"));
    assertTrue(html.contains("Найкраще пояснення каскадів"));
    assertTrue(html.contains("href=\"/books/7\""));
    assertTrue(html.contains("Java Persistence with Hibernate"));
    assertFalse(html.contains("Зрозуміла різницю")); // Comment of another user.
    assertTrue(html.indexOf("Каталог рефакторингів") < html.indexOf("Після цієї книги")); // Newest first.
  }

  @Test void usersPageIsForAdminAndNeverShowsHashes() throws Exception {
    var admin = admin();
    var list = admin.get("/users");
    assertEquals(200, list.statusCode());
    assertTrue(list.body().contains("olena@bookshelf.test") && list.body().contains("ADMIN"));
    assertFalse(list.body().contains("$2a$")); // Password hashes are never rendered.
    var alias = admin.get("/user/olena@bookshelf.test");
    assertEquals(302, alias.statusCode());
    assertTrue(location(alias).endsWith("/users/3/comments"));
    assertEquals(404, admin.follow(admin.get("/users/999/comments")).statusCode());
    assertEquals(403, user().get("/users").statusCode());
  }

  @Test void bookLinkEditAndDeleteUseServiceLayer() throws Exception {
    long id = services.catalog().add("CRUD book", "Tester", 2020).id();
    var admin = admin();
    var show = admin.get("/books/" + id);
    assertEquals(302, show.statusCode());
    assertTrue(location(show).endsWith("/comments?bookId=" + id));
    var form = admin.get("/books/" + id + "/edit");
    assertTrue(form.body().contains("value=\"CRUD book\""));
    assertTrue(form.body().contains("action=\"/books/" + id + "/edit\""));
    var edited = admin.post("/books/" + id + "/edit", Map.of("id", "999", "title", "CRUD book 2",
        "author", "Tester", "pubYear", "2021"));
    assertEquals(302, edited.statusCode());
    assertEquals("CRUD book 2", services.catalog().get(id).title());
    services.comments().add(id, 4, "Will be removed by cascade");
    assertEquals(302, admin.post("/books/" + id + "/delete", Map.of()).statusCode());
    assertEquals(404, admin.follow(admin.get("/comments?bookId=" + id)).statusCode());
  }

  /** Complex lab: Render checks /actuator/health without a session; metrics are for ADMIN. */
  @Test void actuatorHealthIsPublicAndMetricsOnlyForAdmin() throws Exception {
    var health = new TestBrowser(port).get("/actuator/health");
    assertEquals(200, health.statusCode());
    assertEquals("{\"status\":\"UP\"}", health.body()); // show-details=never
    assertTrue(new TestBrowser(port).get("/actuator/info").body().contains("BookApp (mpf-labs)"));
    assertEquals(200, admin().get("/actuator/metrics/http.server.requests").statusCode());
    assertEquals(403, user().get("/actuator/metrics").statusCode());
    assertEquals(302, new TestBrowser(port).get("/actuator/metrics").statusCode());
  }

  @Test void staticStylesheetIsPublic() throws Exception {
    assertEquals(200, new TestBrowser(port).get("/css/catalog.css").statusCode());
  }
}
