package sumdu.edu.ua.web.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.port.MailPort;

/**
 * Letters rendered with FreeMarker and sent through the Resend HTTP API (project Mail-fix):
 * Render blocks the SMTP ports, so SMTP stays on the provider's side, not in the application.
 */
@Service
public class MailService implements MailPort {
  private static final Logger log = LoggerFactory.getLogger(MailService.class);
  static final String RESEND_API_URL = "https://api.resend.com/emails";
  static final String VERIFY_SUBJECT = "Підтвердження реєстрації в BookApp";
  private final RestTemplate restTemplate;
  private final EmailTemplateProcessor templates;
  private final Clock clock;
  private final String apiKey, from, to, baseUrl;

  public MailService(RestTemplateBuilder rest, EmailTemplateProcessor templates, Clock clock,
      @Value("${mail.resend.api-key}") String apiKey, @Value("${mail.resend.from}") String from,
      @Value("${app.mail.admin}") String to, @Value("${app.base-url}") String baseUrl) {
    this.restTemplate = rest.build();
    this.templates = templates; this.clock = clock;
    this.apiKey = apiKey; this.from = from; this.to = to; this.baseUrl = baseUrl;
  }

  /** Used by tests to bind MockRestServiceServer. */
  RestTemplate restTemplate() {
    return restTemplate;
  }

  public void sendNewBookEmail(Book book) {
    var model = Map.<String, Object>of(
        "id", book.id(), "title", book.title(), "author", book.author(),
        "year", book.pubYear(), "added", Date.from(clock.instant()),
        "footerYear", clock.instant().atZone(clock.getZone()).getYear(),
        "comments", List.of(), "baseUrl", baseUrl);
    if (!configured("Нова книга в каталозі")) return;
    sendHtml(to, "Нова книга в каталозі", templates.process("new_book.ftl", model));
    log.info("Email sent: bookId={}, recipient={}, subject=Нова книга в каталозі", book.id(), to);
  }

  /** Letter with the one-time link /confirm?token=... */
  @Override
  public void sendVerificationEmail(String email, String token) {
    String link = baseUrl + "/confirm?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    var model = Map.<String, Object>of(
        "mailTitle", VERIFY_SUBJECT, "email", email, "link", link,
        "footerYear", clock.instant().atZone(clock.getZone()).getYear(),
        "footerNote", "Лист надіслано під час реєстрації в каталозі книг");
    String html = templates.process("verify_account.ftl", model);
    if (!configured(VERIFY_SUBJECT)) {
      // Development fallback only: without a mail provider the link is the one way to confirm.
      log.warn("Mail provider is not configured, confirmation link for {}: {}", email, link);
      return;
    }
    sendHtml(email, VERIFY_SUBJECT, html);
    log.info("Verification email sent: recipient={}", email); // The token is never logged.
  }

  private boolean configured(String subject) {
    if (apiKey != null && !apiKey.isBlank()) return true;
    log.error("Resend API key is not configured. Cannot send email '{}'", subject);
    return false;
  }

  private void sendHtml(String recipient, String subject, String html) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);
    var body = Map.<String, Object>of(
        "from", from, "to", List.of(recipient), "subject", subject, "html", html);
    try {
      var response = restTemplate.postForEntity(
          RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
      log.debug("Resend accepted the email: {}", response.getBody());
    } catch (RestClientException e) {
      // 4xx/5xx of Resend (for example, a recipient outside the verified domain) or no network.
      log.error("Email sending failed: recipient={}, type={}, message={}", recipient,
          e.getClass().getSimpleName(), e.getMessage());
      throw new IllegalStateException("Email sending failed", e);
    }
  }
}
