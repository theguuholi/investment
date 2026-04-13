package com.github.theguuholi.investment.montadora.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.theguuholi.investment.montadora.Motor;
import com.github.theguuholi.investment.montadora.TipoMotor;

//  @Configuration marks a class as a source of Spring Beans.
@Configuration
// Spring reads this class at startup and registers the returned objects into the Application Context 
// — making them available for injection anywhere via @Autowired or constructor injection.
public class MontadoraConfiguration {
    // it will use the component scan to find the beans defined in this package and its subpackages,

    @Bean // it could be an email service, a repository, or any other component that your application needs.
    @Primary // if you have multiple beans of the same type, you can use @Primary to indicate which one should be injected by default when there is a conflict. In this case, if you inject a Motor bean without specifying which one, Spring will inject the motorEletrico bean because it is marked as @Primary.
    // ⏺ @Bean tells Spring: "manage this object for me". 
    public Motor motorEletrico() {
        // When Spring sees this method, it will call it and register the returned Motor object as a bean in the application context.
        return new Motor("1.5 Turbo", 173, 4, 1.5, TipoMotor.TURBO);
    }

    @Bean(name = "motorHibrido") // you can specify a custom name for the bean, otherwise it will default to the method name. IT IS NOT REQUIRED
    public Motor motorHibrido() {
        return new Motor("2.0 Híbrido", 200, 4, 2.0, TipoMotor.HIBRIDO);
    }

    @Bean(name = "motorAspirado")
    public Motor  motorAspirado() {
        return new Motor("2.0 Aspirado", 150, 4, 2.0, TipoMotor.ASPIRADO);
    }
}
