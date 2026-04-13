package com.github.theguuholi.investment.montadora;

public record Motor(String modelo, int cavalos, int cilindros, double litragem, TipoMotor tipoMotor) {

    @Override
    public String toString() {
        return "Motor{modelo='%s', cavalos=%d, cilindros=%d, litragem=%.1fL, tipo=%s}"
                .formatted(modelo, cavalos, cilindros, litragem, tipoMotor);
    }
}
