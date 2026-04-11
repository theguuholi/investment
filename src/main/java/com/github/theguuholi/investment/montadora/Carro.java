package com.github.theguuholi.investment.montadora;

import java.awt.Color;

import com.github.theguuholi.investment.montadora.api.CarroStatus;

public class Carro {
    private String modelo;
    private Color cor;
    private Motor motor;
    private Montadora montadora;

    public Carro(Motor motor) {
        this.motor = motor;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Color getCor() {
        return cor;
    }

    public void setCor(Color cor) {
        this.cor = cor;
    }

    public Motor getMotor() {
        return motor;
    }

    public void setMotor(Motor motor) {
        this.motor = motor;
    }

    public Montadora getMontadora() {
        return montadora;
    }

    public void setMontadora(Montadora montadora) {
        this.montadora = montadora;
    }

    public CarroStatus darIgnicao(Chave chave) {
        if(chave.montadora() == this.montadora) {
            return new CarroStatus(true, "Carro ligado com sucesso!" + " Motor: " + this.motor);
        } else {
            return new CarroStatus(false, "Chave incompatível. O carro não pode ser ligado.");
        }
    }


    

}
