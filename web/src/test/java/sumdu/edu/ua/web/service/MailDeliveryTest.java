package sumdu.edu.ua.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.web.config.EmailFreemarkerConfig;

/** Resend HTTP API (Mail-fix): JSON request, Bearer key, errors and the no-key fallback. */
class MailDeliveryTest {
  MockRestServiceServer server;

  MailService service(String apiKey) {
    var processor = new EmailTemplateProcessor(
        new EmailFreemarkerConfig().emailFreemarkerConfiguration("UTC"));
    var service = new MailService(new RestTemplateBuilder(), processor,
        Clock.fixed(Instant.parse("2026-11-02T09:30:00Z"), ZoneOffset.UTC), apiKey,
        "Book App <onboarding@resend.dev>", "reader@localhost.test", "https://mpf-labs.onrender.com");
    server = MockRestServiceServer.bindTo(service.restTemplate()).build();
    return service;
  }

  @Test void newBookLetterIsPostedToResendWithBearerKey() {
    var service = service("re_test_key");
    server.expect(requestTo(MailService.RESEND_API_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer re_test_key"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.from").value("Book App <onboarding@resend.dev>"))
        .andExpect(jsonPath("$.to[0]").value("reader@localhost.test"))
        .andExpect(jsonPath("$.subject").value("Нова книга в каталозі"))
        .andExpect(jsonPath("$.html").value(org.hamcrest.Matchers.containsString("Раритетне видання!")))
        .andRespond(withSuccess("{\"id\":\"4ef9a417\"}", MediaType.APPLICATION_JSON));
    service.sendNewBookEmail(new Book(123, "Книга", "Автор", 1911));
    server.verify();
  }

  @Test void verificationLetterGoesToTheNewAccountWithConfirmLink() {
    var service = service("re_test_key");
    server.expect(requestTo(MailService.RESEND_API_URL))
        .andExpect(jsonPath("$.to[0]").value("taras@bookshelf.test"))
        .andExpect(jsonPath("$.subject").value("Підтвердження реєстрації в BookApp"))
        .andExpect(jsonPath("$.html").value(org.hamcrest.Matchers.containsString(
            "https://mpf-labs.onrender.com/confirm?token=0f6c2a54-token")))
        .andRespond(withSuccess("{\"id\":\"9a1c\"}", MediaType.APPLICATION_JSON));
    service.sendVerificationEmail("taras@bookshelf.test", "0f6c2a54-token");
    server.verify();
  }

  @Test void resendErrorIsPropagatedToController() {
    var service = service("re_test_key");
    server.expect(requestTo(MailService.RESEND_API_URL)).andRespond(withStatus(
        org.springframework.http.HttpStatus.FORBIDDEN).body("{\"message\":\"domain is not verified\"}"));
    assertThrows(IllegalStateException.class,
        () -> service.sendNewBookEmail(new Book(123, "Книга", "Автор", 2026)));
  }

  @Test void withoutApiKeyNothingIsSentAndNoExceptionIsThrown() {
    var service = service("");
    assertDoesNotThrow(() -> service.sendVerificationEmail("taras@bookshelf.test", "token"));
    assertDoesNotThrow(() -> service.sendNewBookEmail(new Book(1, "Книга", "Автор", 2026)));
    server.verify(); // No request was expected.
  }
}
