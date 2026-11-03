package ua.edu.sumdu.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import ua.edu.sumdu.persistence.jpa.PersistenceConfig;

/** Minimal Boot context for the persistence module: DataSource, Flyway, JPA, adapters. */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(PersistenceConfig.class)
class JpaTestApplication {}
