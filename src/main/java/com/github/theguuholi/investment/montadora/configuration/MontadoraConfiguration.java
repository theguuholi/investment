package com.github.theguuholi.investment.montadora.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.theguuholi.investment.montadora.Motor;
import com.github.theguuholi.investment.montadora.TipoMotor;

//  @Configuration marks a class as a source of Spring Beans.
@Configuration
// Spring reads this class at startup and registers the returned objects into the Application Context 
// — making them available for injection anywhere via @Autowired or constructor injection.
public class MontadoraConfiguration {
    // it will use the component scan to find the beans defined in this package and its subpackages,

    @Bean // it could be an email service, a repository, or any other component that your application needs.
    // ⏺ @Bean tells Spring: "manage this object for me". 
    public Motor motor() {
        // When Spring sees this method, it will call it and register the returned Motor object as a bean in the application context.
        return new Motor("1.5 Turbo", 173, 4, 1.5, TipoMotor.TURBO);
    }
}
