package ua.edu.sumdu.persistence;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.*;
import org.junit.jupiter.api.Test;

class PersistenceArchitectureTest {
  @Test
  void persistenceNeverDependsOnWebOrServlets() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ua.edu.sumdu", "sumdu.edu.ua.core");
    noClasses()
        .that()
        .resideInAPackage("..persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..web..", "jakarta.servlet..")
        .check(classes);
  }

  @Test
  void repositoryImplementationsStayInPersistence() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ua.edu.sumdu", "sumdu.edu.ua.core");
    classes()
        .that()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .should()
        .resideInAPackage("ua.edu.sumdu.persistence.jpa.repository")
        .andShould()
        .beInterfaces()
        .check(classes);
  }

  @Test
  void noModuleUsesJdbcDirectly() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ua.edu.sumdu", "sumdu.edu.ua.core");
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.sql..", "javax.sql..", "org.springframework.jdbc..")
        .check(classes);
  }

  @Test
  void entitiesLiveOnlyInPersistence() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ua.edu.sumdu", "sumdu.edu.ua.core");
    classes()
        .that()
        .areAnnotatedWith(jakarta.persistence.Entity.class)
        .should()
        .resideInAPackage("ua.edu.sumdu.persistence.jpa.entity")
        .check(classes);
    noClasses()
        .that()
        .resideOutsideOfPackage("ua.edu.sumdu.persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("ua.edu.sumdu.persistence..")
        .check(classes);
  }
}
