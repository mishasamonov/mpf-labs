package sumdu.edu.ua.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.error.*;
import sumdu.edu.ua.core.port.CommentRepositoryPort;

/** Lab 10: unit tests of CommentService with a Mockito mock of the repository port. */
class CommentServiceTest {
  static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
  static final long BOOK_ID = 1L, COMMENT_ID = 2L;

  CommentRepositoryPort repo;
  CommentService service;

  @BeforeEach
  void setUp() {
    repo = mock(CommentRepositoryPort.class);
    service = new CommentService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  void stored(Instant createdAt) {
    when(repo.bookExists(BOOK_ID)).thenReturn(true);
    when(repo.findById(BOOK_ID, COMMENT_ID)).thenReturn(Optional.of(
        new Comment(COMMENT_ID, BOOK_ID, 7L, "olena", "Корисна книга", createdAt)));
    when(repo.delete(BOOK_ID, COMMENT_ID)).thenReturn(true);
  }

  @Nested
  @DisplayName("delete(bookId, commentId, createdAt)")
  class Delete {
    @ParameterizedTest(name = "bookId={0}, commentId={1}")
    @CsvSource({"0, 2", "-1, 2", "1, 0", "1, -5", "0, 0"})
    void invalidIdsThrowAndRepositoryIsNotCalled(long bookId, long commentId) {
      var e = assertThrows(InvalidCommentDeleteException.class,
          () -> service.delete(bookId, commentId, NOW.minusSeconds(60)));
      assertTrue(e.getMessage().contains("мають бути додатними"));
      verifyNoInteractions(repo);
    }

    @Test
    void missingCreatedAtThrows() {
      var e = assertThrows(InvalidCommentDeleteException.class,
          () -> service.delete(BOOK_ID, COMMENT_ID, null));
      assertEquals("Не вказано час створення коментаря (createdAt)", e.getMessage());
      verifyNoInteractions(repo);
    }

    @Test
    void commentOlderThan24HoursThrows() {
      var e = assertThrows(CommentTooOldException.class,
          () -> service.delete(BOOK_ID, COMMENT_ID, NOW.minus(Duration.ofHours(25))));
      assertEquals("Коментар створено більше ніж 24 години тому і не може бути видалений",
          e.getMessage());
      verifyNoInteractions(repo);
    }

    @Test
    void exactly24HoursIsAlreadyTooOld() {
      assertThrows(CommentTooOldException.class,
          () -> service.delete(BOOK_ID, COMMENT_ID, NOW.minus(Duration.ofHours(24))));
      verifyNoInteractions(repo);
    }

    @Test
    void futureCreatedAtIsInvalid() {
      assertThrows(InvalidCommentDeleteException.class,
          () -> service.delete(BOOK_ID, COMMENT_ID, NOW.plusSeconds(1)));
      verifyNoInteractions(repo);
    }

    @Test
    void validDataCallsRepositoryDelete() {
      Instant createdAt = NOW.minus(Duration.ofHours(23)).minusSeconds(59 * 60);
      stored(createdAt);
      service.delete(BOOK_ID, COMMENT_ID, createdAt);
      verify(repo).delete(BOOK_ID, COMMENT_ID);
    }

    @Test
    void validationHappensBeforeAnyRepositoryCall() {
      Instant createdAt = NOW.minusSeconds(30);
      stored(createdAt);
      service.delete(BOOK_ID, COMMENT_ID, createdAt);
      var order = inOrder(repo);
      order.verify(repo).bookExists(BOOK_ID);
      order.verify(repo).findById(BOOK_ID, COMMENT_ID);
      order.verify(repo).delete(BOOK_ID, COMMENT_ID);
      verifyNoMoreInteractions(repo);
    }

    @Test
    void missingBookThrowsBookNotFound() {
      when(repo.bookExists(99L)).thenReturn(false);
      var e = assertThrows(BookNotFoundException.class,
          () -> service.delete(99L, COMMENT_ID, NOW.minusSeconds(60)));
      assertEquals("Книгу з id=99 не знайдено", e.getMessage());
      verify(repo, never()).delete(anyLong(), anyLong());
    }

    @Test
    void missingCommentThrowsCommentNotFound() {
      when(repo.bookExists(BOOK_ID)).thenReturn(true);
      when(repo.findById(BOOK_ID, COMMENT_ID)).thenReturn(Optional.empty());
      assertThrows(CommentNotFoundException.class,
          () -> service.delete(BOOK_ID, COMMENT_ID, NOW.minusSeconds(60)));
      verify(repo, never()).delete(anyLong(), anyLong());
    }

    @Test
    void forgedCreatedAtCannotBypassTheRule() {
      stored(NOW.minus(Duration.ofDays(30))); // The real comment is a month old.
      var e = assertThrows(InvalidCommentDeleteException.class,
          () -> service.delete(BOOK_ID, COMMENT_ID, NOW.minusSeconds(60)));
      assertEquals("Час створення не збігається зі збереженим коментарем", e.getMessage());
      verify(repo, never()).delete(anyLong(), anyLong());
    }
  }

  @Nested
  @DisplayName("validateText(text) — extension")
  class ValidateText {
    @Test
    void trimsCorrectText() {
      assertEquals("Чудова книга", service.validateText("  Чудова книга \n"));
      verifyNoInteractions(repo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\t"})
    void blankTextIsRejected(String text) {
      var e = assertThrows(InvalidCommentTextException.class, () -> service.validateText(text));
      assertEquals("Текст коментаря не може бути порожнім", e.getMessage());
    }

    @Test
    void nullTextIsRejected() {
      assertThrows(InvalidCommentTextException.class, () -> service.validateText(null));
    }

    @Test
    void textLongerThan1000CharactersIsRejected() {
      assertEquals(1000, service.validateText("я".repeat(1000)).length());
      var e = assertThrows(InvalidCommentTextException.class,
          () -> service.validateText("я".repeat(1001)));
      assertEquals("Текст коментаря довший за 1000 символів (1001)", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Найкраще КАЗИНО онлайн | казино",
        "Це спам, не читайте | спам",
        "Buy now: SPAM! | spam"})
    void forbiddenWordsAreRejectedCaseInsensitively(String text, String word) {
      var e = assertThrows(InvalidCommentTextException.class, () -> service.validateText(text));
      assertEquals("Коментар містить заборонене слово «" + word + "»", e.getMessage());
    }

    @Test
    void wordsThatOnlyContainForbiddenOnesAreAllowed() {
      assertEquals("Антиспамові фільтри описано добре",
          service.validateText("Антиспамові фільтри описано добре"));
    }

    @Test
    void invalidTextIsAValidationException() {
      assertInstanceOf(sumdu.edu.ua.core.error.ValidationException.class,
          assertThrows(InvalidCommentTextException.class, () -> service.validateText(" ")));
    }
  }

  @Test
  void methodologyConstructorUsesSystemClock() {
    var real = new CommentService(repo);
    assertThrows(CommentTooOldException.class,
        () -> real.delete(BOOK_ID, COMMENT_ID, Instant.now().minus(Duration.ofDays(2))));
    verifyNoInteractions(repo);
  }
}
