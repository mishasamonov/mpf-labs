package sumdu.edu.ua.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class WebArchitectureTest {
  @Test
  void controllersUseServicesNotJdbcEntitiesOrRepositories() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("sumdu.edu.ua.web");
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "java.sql..",
            "javax.sql..",
            "org.springframework.jdbc..",
            "ua.edu.sumdu.persistence.jpa.entity..",
            "ua.edu.sumdu.persistence.jpa.repository..",
            "ua.edu.sumdu.persistence.jpa.adapter..")
        .check(classes);
  }
}
