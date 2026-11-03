package sumdu.edu.ua.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.test.context.support.WithMockUser;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.error.CommentTooOldException;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.service.CommentService;

/**
 * Lab 10: the real CommentService bean behind the AOP proxy, with a mocked repository port.
 * Checks the log lines of CommentServiceLoggingAspect and ServiceTimingAspect.
 */
@SpringBootTest(classes = AppInit.class,
    properties = {"spring.datasource.url=jdbc:h2:mem:lab10aop;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "catalog.clock-zone=UTC"})
@ExtendWith(OutputCaptureExtension.class)
@WithMockUser(roles = "ADMIN")
class CommentServiceAspectIT {
  @Autowired CommentService commentService;
  @Autowired Clock clock;
  @MockBean CommentRepositoryPort repo;

  @Test
  void serviceBeanIsAnAopProxy() {
    assertTrue(AopUtils.isAopProxy(commentService));
    assertTrue(AopUtils.isCglibProxy(commentService)); // No interface: a CGLIB subclass.
  }

  @Test
  void successfulDeleteIsLoggedWithArgumentsAndTime(CapturedOutput output) {
    Instant createdAt = clock.instant().minus(Duration.ofMinutes(5));
    when(repo.bookExists(1L)).thenReturn(true);
    when(repo.findById(1L, 2L)).thenReturn(Optional.of(
        new Comment(2L, 1L, 3L, "olena", "AOP", createdAt)));
    when(repo.delete(1L, 2L)).thenReturn(true);

    commentService.delete(1L, 2L, createdAt);

    verify(repo).delete(1L, 2L);
    assertTrue(output.getOut().contains("Calling CommentService.delete(bookId=1, commentId=2)"));
    assertTrue(output.getOut().matches(
        "(?s).*CommentService\\.delete\\(bookId=1, commentId=2\\) finished in \\d+ ms.*"));
    assertTrue(output.getOut().contains("[timing] CommentService.delete took in"));
  }

  @Test
  void failedDeleteIsLoggedAndExceptionIsNotSwallowed(CapturedOutput output) {
    Instant old = clock.instant().minus(Duration.ofDays(3));

    assertThrows(CommentTooOldException.class, () -> commentService.delete(5L, 6L, old));

    verifyNoInteractions(repo);
    assertTrue(output.getOut().matches("(?s).*CommentService\\.delete\\(bookId=5, commentId=6\\) "
        + "failed in \\d+ ms: Коментар створено більше ніж 24 години тому.*"));
    assertTrue(output.getOut().contains("[timing] CommentService.delete failed in"));
  }

  @Test
  void timingAspectCoversOtherServiceMethods(CapturedOutput output) {
    assertEquals("Текст", commentService.validateText("  Текст "));
    assertTrue(output.getOut().contains("[timing] CommentService.validateText took in"));
    assertFalse(output.getOut().contains("Calling CommentService.validateText")); // Only delete.
  }
}
