package ua.edu.sumdu.persistence.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Entry point of the persistence module: entities, Spring Data repositories, port adapters. */
@Configuration
@EntityScan("ua.edu.sumdu.persistence.jpa.entity")
@EnableJpaRepositories("ua.edu.sumdu.persistence.jpa.repository")
@ComponentScan("ua.edu.sumdu.persistence.jpa.adapter")
public class PersistenceConfig {}
