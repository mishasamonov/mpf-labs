package sumdu.edu.ua.web;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Browser-like HTTP session for integration tests: cookie jar (JSESSIONID), no redirects,
 * and the CSRF token taken from the last rendered page, exactly like a real form submit.
 */
final class TestBrowser {
  static final String ADMIN = "admin@bookshelf.test", ADMIN_PASSWORD = "admin123";
  static final String USER = "olena@bookshelf.test", USER_PASSWORD = "user123";
  private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
  private final int port;
  private final HttpClient client = HttpClient.newBuilder()
      .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
  private String csrf;

  TestBrowser(int port) {
    this.port = port;
  }

  static TestBrowser login(int port, String email, String password) throws Exception {
    var browser = new TestBrowser(port);
    browser.get("/login");
    var response = browser.post("/login", Map.of("email", email, "password", password));
    assertEquals(302, response.statusCode());
    assertEquals("/books", URI.create(location(response)).getPath());
    browser.get("/books"); // The session id and CSRF token change after login.
    return browser;
  }

  HttpResponse<String> get(String path, String... headers) throws Exception {
    return remember(send(path, null, headers));
  }

  HttpResponse<String> post(String path, Map<String, String> fields, String... headers)
      throws Exception {
    var body = new LinkedHashMap<>(fields);
    if (csrf != null) body.put("_csrf", csrf);
    return remember(send(path, form(body), headers));
  }

  /** Lab 10: GlobalMvcExceptionHandler answers 302; a browser then opens /error-page. */
  HttpResponse<String> follow(HttpResponse<String> response) throws Exception {
    assertEquals(302, response.statusCode());
    var target = URI.create(location(response));
    return get(target.getRawPath() + (target.getRawQuery() == null ? "" : "?" + target.getRawQuery()));
  }

  HttpResponse<String> postWithoutCsrf(String path, Map<String, String> fields) throws Exception {
    return send(path, form(fields));
  }

  private HttpResponse<String> send(String path, String body, String... headers) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    for (int i = 0; i < headers.length; i += 2) request.header(headers[i], headers[i + 1]);
    if (body != null) request.header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body));
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> remember(HttpResponse<String> response) {
    var m = CSRF.matcher(response.body());
    if (m.find()) csrf = m.group(1);
    return response;
  }

  /** Stateless request to /api/** like Postman: optional HTTP Basic, optional JSON body. */
  static HttpResponse<String> api(int port, String method, String path, String json,
      String email, String password) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Accept", "application/json")
        .method(method, json == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json));
    if (json != null) request.header("Content-Type", "application/json");
    if (email != null) request.header("Authorization", "Basic " + Base64.getEncoder()
        .encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8)));
    return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  static String location(HttpResponse<?> response) {
    return response.headers().firstValue("Location").orElseThrow();
  }

  static String form(Map<String, String> fields) {
    return fields.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
        .collect(Collectors.joining("&"));
  }

  static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
