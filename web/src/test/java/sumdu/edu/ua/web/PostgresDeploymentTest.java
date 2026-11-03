package sumdu.edu.ua.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Complex lab: the production configuration against a real PostgreSQL. No datasource override:
 * the URL, user and password come from SPRING_DATASOURCE_* (GitHub Actions service container)
 * or from the defaults that match compose.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresDeploymentTest {
  @Autowired JdbcTemplate jdbc;
  @LocalServerPort int port;

  @Test void applicationRunsOnPostgresql() {
    assertTrue(jdbc.queryForObject("SELECT version()", String.class).startsWith("PostgreSQL"));
  }

  /** ddl-auto=validate has already passed: the context would not start otherwise. */
  @Test void allFlywayMigrationsAreAppliedSuccessfully() {
    var versions = jdbc.queryForList(
        "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
        String.class);
    assertEquals(java.util.List.of("1", "2", "3", "4", "5"), versions);
    assertEquals(0, jdbc.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE NOT success", Integer.class));
  }

  @Test void seedDataAndIdentityColumnsAreReady() {
    assertEquals("Чистий код", jdbc.queryForObject("SELECT title FROM books WHERE id = 1", String.class));
    assertEquals("ADMIN", jdbc.queryForObject(
        "SELECT role FROM users WHERE email = 'admin@bookshelf.test'", String.class));
    assertTrue(jdbc.queryForObject("SELECT MAX(id) FROM books", Long.class) >= 7);
  }

  @Test void healthEndpointIsUpWithDatabase() throws Exception {
    var health = new TestBrowser(port).get("/actuator/health");
    assertEquals(200, health.statusCode());
    assertEquals("{\"status\":\"UP\"}", health.body());
  }
}
