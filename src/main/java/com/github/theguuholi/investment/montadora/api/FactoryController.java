package com.github.theguuholi.investment.montadora.api;

import org.springframework.web.bind.annotation.RestController;

import com.github.theguuholi.investment.montadora.Carro;
import com.github.theguuholi.investment.montadora.Chave;
import com.github.theguuholi.investment.montadora.HondaHRV;
import com.github.theguuholi.investment.montadora.Motor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController()
public class FactoryController {

    @Autowired 
    // spring go to your instance and get a reference to the Motor bean that you defined in your configuration class and inject it into this field.
    private Motor motor;
    
    @PostMapping("/ligar")
    public CarroStatus ligar(@RequestBody Chave chave) {
        var carro = new HondaHRV(motor);  
        CarroStatus status = carro.darIgnicao(chave); 
        return status;
    }
    
}
