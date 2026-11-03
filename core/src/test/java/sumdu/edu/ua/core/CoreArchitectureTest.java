package sumdu.edu.ua.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;

import com.tngtech.archunit.core.importer.*;
import org.junit.jupiter.api.Test;

class CoreArchitectureTest {
  @Test
  void coreHasNoInfrastructureDependencies() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("sumdu.edu.ua.core");
    noClasses()
        .should()
        .dependOnClassesThat(
            resideInAnyPackage(
                    "jakarta.servlet..",
                    "javax.servlet..",
                    "java.sql..",
                    "javax.sql..",
                    "org.h2..",
                    "org.hibernate..",
                    "org.springframework..",
                    "jakarta.persistence..",
                    "..persistence..",
                    "..web..")
                // Lab 9: only the Spring Security API (UserDetailsService, @PreAuthorize, BCrypt).
                .and(not(resideInAnyPackage(
                    "org.springframework.security.core..",
                    "org.springframework.security.access..",
                    "org.springframework.security.crypto.."))))
        .check(classes);
  }

  @Test
  void allCoreConstructorsArePublic() {
    var classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("sumdu.edu.ua.core");
    constructors()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..core..")
        .should()
        .bePublic()
        .check(classes);
  }
}
