package com.github.theguuholi.investment.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.github.theguuholi.investment")
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_shouldOnlyBeInApiPackages = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("..api..")
            .orShould().resideInAPackage("..auth..")
            .because("Controllers must live in api or auth packages");

    @ArchTest
    static final ArchRule services_shouldNotDependOnControllers = noClasses()
            .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
            .should().dependOnClassesThat()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .because("Services must not depend on controllers");

    @ArchTest
    static final ArchRule repositories_shouldNotBeAccessedByControllers = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat()
            .areAssignableTo(org.springframework.data.repository.Repository.class)
            .allowEmptyShould(true)
            .because("Controllers must not access repositories directly — go through services");

    @ArchTest
    static final ArchRule entities_shouldBeInDomainPackages = classes()
            .that().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().resideInAPackage("com.github.theguuholi.investment.(*)..")
            .allowEmptyShould(true)
            .because("Entities must be in feature packages");

    @ArchTest
    static final ArchRule api_packagesShouldNotDependOnEachOther = noClasses()
            .that().resideInAPackage("..user.api..")
            .should().dependOnClassesThat().resideInAPackage("..portfolio..")
            .orShould().dependOnClassesThat().resideInAPackage("..asset..")
            .because("Feature controllers must not cross-depend on other feature packages");
}
