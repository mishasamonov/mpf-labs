package sumdu.edu.ua.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.error.*;
import sumdu.edu.ua.core.service.CommentService;

/**
 * Lab 10: the web layer with a mocked CommentService. Shows that exceptions thrown by the core
 * service become HTTP responses in GlobalApiExceptionHandler.
 */
@SpringBootTest(classes = AppInit.class,
    properties = {"spring.datasource.url=jdbc:h2:mem:lab10mockmvc;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "catalog.clock-zone=UTC"})
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN") // POST /comments/delete is for ADMIN since lab 9.
class CommentDeleteExceptionIT {
  static final String CREATED_AT = "2024-01-01T00:00:00Z";

  @Autowired MockMvc mockMvc;
  @MockBean CommentService commentService;

  @Test
  void tooOldCommentGives400WithJsonError() throws Exception {
    doThrow(new CommentTooOldException())
        .when(commentService).delete(eq(1L), eq(2L), any());

    mockMvc.perform(post("/comments/delete").with(csrf())
            .param("bookId", "1")
            .param("commentId", "2")
            .param("createdAt", CREATED_AT))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error")
            .value("Коментар створено більше ніж 24 години тому і не може бути видалений"));

    verify(commentService).delete(1L, 2L, Instant.parse(CREATED_AT)); // Parsed from the form.
  }

  @Test
  void invalidIdsGive400WithJsonError() throws Exception {
    doThrow(new InvalidCommentDeleteException("Ідентифікатори книги та коментаря мають бути додатними"))
        .when(commentService).delete(eq(0L), anyLong(), any());

    mockMvc.perform(post("/comments/delete").with(csrf())
            .param("bookId", "0").param("commentId", "2").param("createdAt", CREATED_AT))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Ідентифікатори книги та коментаря мають бути додатними"));
  }

  @Test
  void missingCreatedAtReachesServiceAsNull() throws Exception {
    doThrow(new InvalidCommentDeleteException("Не вказано час створення коментаря (createdAt)"))
        .when(commentService).delete(1L, 2L, null);

    mockMvc.perform(post("/comments/delete").with(csrf())
            .param("bookId", "1").param("commentId", "2"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Не вказано час створення коментаря (createdAt)"));
  }

  @Test
  void missingBookGives404WithJsonError() throws Exception {
    doThrow(new BookNotFoundException(42)).when(commentService).delete(eq(42L), anyLong(), any());

    mockMvc.perform(post("/comments/delete").with(csrf())
            .param("bookId", "42").param("commentId", "2").param("createdAt", CREATED_AT))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Книгу з id=42 не знайдено"));
  }

  @Test
  void validDeleteRedirectsBackToComments() throws Exception {
    mockMvc.perform(post("/comments/delete").with(csrf())
            .param("bookId", "1").param("commentId", "2").param("createdAt", CREATED_AT))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/comments?bookId=1"))
        .andExpect(flash().attribute("commentDeleted", true));

    verify(commentService).delete(1L, 2L, Instant.parse(CREATED_AT));
  }

  @Test
  @WithMockUser(roles = "USER")
  void userIsForbiddenAndServiceIsNotCalled() throws Exception {
    mockMvc.perform(post("/comments/delete").with(csrf())
            .param("bookId", "1").param("commentId", "2").param("createdAt", CREATED_AT))
        .andExpect(status().isForbidden());

    verifyNoInteractions(commentService);
  }

  @Test
  void requestWithoutCsrfTokenIsRejected() throws Exception {
    mockMvc.perform(post("/comments/delete")
            .param("bookId", "1").param("commentId", "2").param("createdAt", CREATED_AT))
        .andExpect(status().isForbidden());

    verifyNoInteractions(commentService);
  }
}
