package sumdu.edu.ua.web;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import sumdu.edu.ua.web.config.EmailFreemarkerConfig;
import sumdu.edu.ua.web.service.EmailTemplateProcessor;

class EmailTemplateTest {
  final EmailTemplateProcessor processor = new EmailTemplateProcessor(
      new EmailFreemarkerConfig().emailFreemarkerConfiguration("Europe/Kyiv"));
  Map<String,Object> model(int year) {
    var m = new HashMap<String,Object>();
    m.put("id", 123); m.put("title", "Тіні забутих предків");
    m.put("author", "Михайло Коцюбинський"); m.put("year", year);
    m.put("added", Date.from(Instant.parse("2026-11-02T09:30:00Z")));
    m.put("footerYear", 2026); m.put("baseUrl", "http://localhost:8087");
    m.put("comments", List.of()); return m;
  }
  String render(Map<String,Object> m) { return processor.process("new_book.ftl", m); }
  @Test void oldBookHasRareBadgeAndFormattedDate() {
    var html=render(model(1911));
    assertTrue(html.contains("Раритетне видання!"));
    assertTrue(html.contains("2026-11-02 11:30"));
    assertTrue(html.contains("1911")); assertFalse(html.contains("1,911"));
    assertTrue(html.contains("Тіні забутих предків"));
  }
  @Test void year2000DoesNotHaveRareBadge() { assertFalse(render(model(2000)).contains("Раритетне видання!")); }
  @Test void modernBookDoesNotHaveRareBadge() { assertFalse(render(model(2026)).contains("Раритетне видання!")); }
  @Test void layoutIncludesLogoFooterAndSavedId() {
    var html=render(model(2026));
    assertTrue(html.contains("raw.githubusercontent.com/VladPiatachenko/MPF_Labs/"));
    assertTrue(html.contains("BookApp © 2026"));
    assertTrue(html.contains("http://localhost:8087/comments?bookId=123"));
    assertTrue(html.contains("background:#eef6ff;border:1px solid"));
  }
  @Test void userInputIsEscapedInEveryTemplate() {
    var m=model(2026);m.put("title","<script>alert(1)</script> & книга");
    m.put("author","<b>Автор</b>");m.put("comments",List.of("<img src=x>"));
    var html=render(m);
    assertTrue(html.contains("&lt;script&gt;"));assertFalse(html.contains("<script>"));
    assertTrue(html.contains("&lt;b&gt;Автор&lt;/b&gt;"));
    assertTrue(html.contains("&lt;img src=x&gt;"));
  }
  @Test void optionalCollectionIsOmittedWhenEmptyAndRenderedWhenPresent() {
    var m=model(2026);assertFalse(render(m).contains("Коментарі:"));
    m.put("comments",List.of("Перший відгук","Другий відгук"));
    var html=render(m);assertTrue(html.contains("<li>Перший відгук</li>"));
    assertTrue(html.contains("<li>Другий відгук</li>"));
    m.remove("comments");assertFalse(render(m).contains("Коментарі:"));
  }
  @Test void missingRequiredValueFailsExplicitly() {
    var m=model(2026);m.remove("title");
    assertThrows(IllegalStateException.class,()->render(m));
  }
}
