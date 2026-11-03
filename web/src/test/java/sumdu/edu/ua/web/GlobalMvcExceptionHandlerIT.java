package sumdu.edu.ua.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Lab 10 (extension): HTML controllers redirect to /error-page with a flash message. */
@SpringBootTest(classes = AppInit.class,
    properties = {"spring.datasource.url=jdbc:h2:mem:lab10mvc;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "catalog.clock-zone=UTC"})
@AutoConfigureMockMvc
@WithMockUser(roles = "USER")
class GlobalMvcExceptionHandlerIT {
  @Autowired MockMvc mockMvc;

  @Test
  void missingBookRedirectsToErrorPageWithFlashMessage() throws Exception {
    mockMvc.perform(get("/comments").param("bookId", "999999"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/error-page"))
        .andExpect(flash().attribute("status", 404))
        .andExpect(flash().attribute("detail", "Книгу не знайдено"))
        .andExpect(flash().attribute("path", "/comments"));
  }

  @Test
  void invalidPageRedirectsWith400() throws Exception {
    mockMvc.perform(get("/books").param("page", "-1"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/error-page"))
        .andExpect(flash().attribute("status", 400))
        .andExpect(flash().attribute("messageKey", "error.invalid"));
  }

  @Test
  void errorPageShowsMessageAndRestoresStatus() throws Exception {
    mockMvc.perform(get("/error-page")
            .flashAttr("status", 404).flashAttr("messageKey", "error.missing")
            .flashAttr("detail", "Книгу не знайдено").flashAttr("path", "/comments"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"))
        .andExpect(content().string(containsString("Книгу або відгук не знайдено.")))
        .andExpect(content().string(containsString("Книгу не знайдено")))
        .andExpect(content().string(containsString("Запит: /comments")));
  }

  @Test
  void errorPageWithoutFlashIsGeneric() throws Exception {
    mockMvc.perform(get("/error-page"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Сторінку не знайдено або запит не підтримується.")));
  }
}
