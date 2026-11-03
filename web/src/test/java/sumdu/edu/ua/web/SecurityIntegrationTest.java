package sumdu.edu.ua.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static sumdu.edu.ua.web.TestBrowser.*;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import sumdu.edu.ua.web.service.CatalogFacade;
import sumdu.edu.ua.web.service.MailService;
import sumdu.edu.ua.core.domain.User;

/** Lab 9 checks: login/logout, sign-up with confirmation, roles, 401/403, API, audit. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:lab10tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "catalog.clock-zone=UTC"})
@WithMockUser(roles = "ADMIN")
@ExtendWith(OutputCaptureExtension.class)
class SecurityIntegrationTest {
  @MockBean MailService mailService;
  @LocalServerPort int port;
  @Autowired CatalogFacade services;
  @Autowired PasswordEncoder passwordEncoder;

  @Test void guestIsRedirectedToLoginPageForCatalogAndComments() throws Exception {
    var guest = new TestBrowser(port);
    for (String path : new String[]{"/books", "/comments?bookId=1", "/users/2/comments", "/"}) {
      var response = guest.get(path);
      assertEquals(302, response.statusCode(), path);
      assertEquals("/login", URI.create(location(response)).getPath(), path);
    }
  }

  @Test void loginFormMatchesSpringSecurityAndHasCsrfToken() throws Exception {
    var html = new TestBrowser(port).get("/login").body();
    assertTrue(html.contains("action=\"/login\"") && html.contains("method=\"post\""));
    assertTrue(html.contains("name=\"email\"") && html.contains("name=\"password\""));
    assertTrue(html.contains("name=\"_csrf\""));
    assertTrue(html.contains("href=\"/register\""));
  }

  @Test void successfulLoginRedirectsToBooksAndLogoutToLoginPage() throws Exception {
    var user = TestBrowser.login(port, USER, USER_PASSWORD);
    var logout = user.post("/logout", Map.of());
    assertEquals(302, logout.statusCode());
    assertEquals("/login?logout", URI.create(location(logout)).getPath() + "?" + URI.create(location(logout)).getQuery());
    assertTrue(new TestBrowser(port).get("/login?logout").body().contains("Ви вийшли із системи"));
    assertEquals(302, user.get("/books").statusCode()); // Session is gone.
  }

  @Test void wrongPasswordAndPostWithoutCsrfAreRejected() throws Exception {
    var guest = new TestBrowser(port);
    guest.get("/login");
    var wrong = guest.post("/login", Map.of("email", USER, "password", "wrong-password"));
    assertEquals(302, wrong.statusCode());
    assertTrue(location(wrong).endsWith("/login?error"));
    var user = TestBrowser.login(port, USER, USER_PASSWORD);
    assertEquals(403, user.postWithoutCsrf("/comments?bookId=1", Map.of("text", "No token")).statusCode());
  }

  @Test void userCanReadButGets403ForAdminActions() throws Exception {
    var user = TestBrowser.login(port, USER, USER_PASSWORD);
    assertEquals(200, user.get("/books").statusCode());
    assertEquals(200, user.get("/comments?bookId=1").statusCode());
    for (String path : new String[]{"/books/new", "/books/add", "/books/1/edit", "/users"}) {
      var response = user.get(path);
      assertEquals(403, response.statusCode(), path);
      assertTrue(response.body().contains("Доступ заборонено"), path);
    }
    assertEquals(403, user.post("/books", Map.of("title", "Hack", "author", "Hack", "pubYear", "2026")).statusCode());
    assertEquals(403, user.post("/books/1/delete", Map.of()).statusCode());
    assertEquals(403, user.post("/books/1/edit", Map.of("title", "X", "author", "Y", "pubYear", "2000")).statusCode());
    assertEquals("Чистий код", services.catalog().get(1).title()); // Nothing changed.
  }

  @Test void adminButtonsAreRenderedOnlyForAdminAndPrincipalNameIsShown() throws Exception {
    var userHtml = TestBrowser.login(port, USER, USER_PASSWORD).get("/books").body();
    assertFalse(userHtml.contains("href=\"/books/add\""));
    assertFalse(userHtml.contains("/edit\""));
    assertFalse(userHtml.contains("/delete\""));
    assertFalse(userHtml.contains("href=\"/users\""));
    assertTrue(userHtml.contains("Вийти") && userHtml.contains("action=\"/logout\""));
    assertTrue(userHtml.contains(">olena@bookshelf.test<")); // principal.name
    assertTrue(userHtml.contains(">USER<"));
    var adminHtml = TestBrowser.login(port, ADMIN, ADMIN_PASSWORD).get("/books").body();
    assertTrue(adminHtml.contains("href=\"/books/add\""));
    assertTrue(adminHtml.contains("/books/1/edit") && adminHtml.contains("/books/1/delete"));
    assertTrue(adminHtml.contains("href=\"/users\""));
    assertTrue(adminHtml.contains(">ADMIN<"));
    var guestHtml = new TestBrowser(port).get("/login").body();
    assertFalse(guestHtml.contains("action=\"/logout\"")); // "Вийти" only when signed in.
    assertTrue(guestHtml.contains("href=\"/register\""));
  }

  @Test void registrationHashesPasswordSendsLinkAndConfirmPromotesToUser() throws Exception {
    var guest = new TestBrowser(port);
    assertTrue(guest.get("/register").body().contains("name=\"nickname\""));
    var response = guest.post("/register",
        Map.of("email", "Taras@Bookshelf.test", "nickname", "Тарас", "password", "secret77"));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Перевірте пошту"));
    assertTrue(response.body().contains("taras@bookshelf.test"));
    var token = ArgumentCaptor.forClass(String.class);
    verify(mailService).sendVerificationEmail(eq("taras@bookshelf.test"), token.capture());

    User saved = services.users().findByEmailOrThrow("taras@bookshelf.test");
    assertEquals(User.ROLE_REGISTERED, saved.getRole());
    assertFalse(saved.isVerified());
    assertTrue(saved.getPassword().startsWith("$2a$10$"));
    assertTrue(passwordEncoder.matches("secret77", saved.getPassword()));

    guest.get("/login");
    var unverified = guest.post("/login", Map.of("email", "taras@bookshelf.test", "password", "secret77"));
    assertTrue(location(unverified).endsWith("/login?unverified"));

    var confirmed = guest.get("/confirm?token=" + token.getValue());
    assertEquals(200, confirmed.statusCode());
    assertTrue(confirmed.body().contains("Email підтверджено"));
    assertEquals(User.ROLE_USER, services.users().findByEmailOrThrow("taras@bookshelf.test").getRole());
    assertTrue(new TestBrowser(port).get("/confirm?token=" + token.getValue()).body().contains("Посилання недійсне"));
    var user = TestBrowser.login(port, "taras@bookshelf.test", "secret77");
    assertTrue(user.get("/books").body().contains("Тарас"));
  }

  @Test void registrationValidatesInputAndDuplicateEmail() throws Exception {
    var guest = new TestBrowser(port);
    guest.get("/register");
    var invalid = guest.post("/register", Map.of("email", "not-an-email", "nickname", "", "password", "123"));
    assertEquals(200, invalid.statusCode());
    assertTrue(invalid.body().contains("Некоректний email"));
    assertTrue(invalid.body().contains("Пароль має бути не менше 6 символів"));
    assertFalse(invalid.body().contains("value=\"123\"")); // Password is not echoed.
    var duplicate = guest.post("/register", Map.of("email", USER, "nickname", "", "password", "secret77"));
    assertTrue(duplicate.body().contains("Користувач з таким email вже існує"));
  }

  @Test void apiRejectsGuestsWith401AndUsesRolesForMethods() throws Exception {
    var guest = api(port, "GET", "/api/books", null, null, null);
    assertEquals(401, guest.statusCode());
    assertTrue(guest.headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Basic"));
    assertTrue(guest.body().contains("\"status\":401"));
    assertEquals(401, api(port, "GET", "/api/books", null, USER, "wrong").statusCode());

    var list = api(port, "GET", "/api/books", null, USER, USER_PASSWORD);
    assertEquals(200, list.statusCode());
    assertTrue(list.body().contains("\"title\":\"Чистий код\""));
    assertEquals(200, api(port, "GET", "/api/books/1", null, USER, USER_PASSWORD).statusCode());

    String json = "{\"title\":\"REST Security\",\"author\":\"Spring\",\"pubYear\":2026}";
    var denied = api(port, "POST", "/api/books", json, USER, USER_PASSWORD);
    assertEquals(403, denied.statusCode());
    assertTrue(denied.body().contains("\"status\":403"));

    var created = api(port, "POST", "/api/books", json, ADMIN, ADMIN_PASSWORD);
    assertEquals(201, created.statusCode());
    String path = created.headers().firstValue("Location").orElseThrow();
    assertEquals(403, api(port, "DELETE", path, null, USER, USER_PASSWORD).statusCode());
    assertEquals(204, api(port, "DELETE", path, null, ADMIN, ADMIN_PASSWORD).statusCode());
    assertEquals(404, api(port, "GET", path, null, ADMIN, ADMIN_PASSWORD).statusCode());
    assertTrue(created.headers().firstValue("Set-Cookie").isEmpty()); // Stateless API.
  }

  @Test void errorPages403And404AreRendered() throws Exception {
    var user = TestBrowser.login(port, USER, USER_PASSWORD);
    var missing = user.get("/no-such-page", "Accept", "text/html"); // As a browser does.
    assertEquals(404, missing.statusCode());
    assertTrue(missing.body().contains("Сторінку не знайдено"));
    assertEquals(403, new TestBrowser(port).get("/403").statusCode());
    assertTrue(user.get("/404").body().contains("404"));
  }

  @Test
  @WithMockUser(username = USER, roles = "USER")
  void preAuthorizeProtectsServicesEvenWithoutUrlRules() {
    assertThrows(AccessDeniedException.class, () -> services.catalog().delete(1));
    assertThrows(AccessDeniedException.class, () -> services.catalog().add("T", "A", 2000));
    assertThrows(AccessDeniedException.class, () -> services.users().findAll());
    assertThrows(AccessDeniedException.class, () -> services.users().changeRole(3, "ADMIN"));
    assertEquals("Чистий код", services.catalog().get(1).title()); // Reading is not restricted.
  }

  @Test
  void adminChangesRolesButNotHisOwn() throws Exception {
    var admin = TestBrowser.login(port, ADMIN, ADMIN_PASSWORD);
    var andrii = services.users().findByEmailOrThrow("andrii@bookshelf.test");
    assertEquals(302, admin.post("/users/" + andrii.getId() + "/role", Map.of("role", "ADMIN")).statusCode());
    assertEquals("ADMIN", services.users().findByEmailOrThrow("andrii@bookshelf.test").getRole());
    admin.post("/users/" + andrii.getId() + "/role", Map.of("role", "USER"));
    admin.post("/users/1/role", Map.of("role", "USER"));
    assertEquals("ADMIN", services.users().findById(1).getRole());
  }

  @Test
  void auditLogRecordsLoginFailureLogoutAndDeniedAccess(CapturedOutput output) throws Exception {
    var user = TestBrowser.login(port, USER, USER_PASSWORD);
    user.get("/books/new");
    user.post("/logout", Map.of());
    var guest = new TestBrowser(port);
    guest.get("/login");
    guest.post("/login", Map.of("email", USER, "password", "bad"));
    assertTrue(output.getOut().contains("LOGIN_SUCCESS user=olena@bookshelf.test"));
    assertTrue(output.getOut().contains("ACCESS_DENIED user=olena@bookshelf.test resource=GET /books/new"));
    assertTrue(output.getOut().contains("LOGOUT user=olena@bookshelf.test"));
    assertTrue(output.getOut().contains("LOGIN_FAILURE user=olena@bookshelf.test reason=BadCredentialsException"));
  }

  @Test
  void anonymousServiceCallWithoutContextIsRejected() {
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
    assertThrows(AuthenticationCredentialsNotFoundException.class,
        () -> services.catalog().delete(1));
  }
}
