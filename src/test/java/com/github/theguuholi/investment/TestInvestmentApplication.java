package com.github.theguuholi.investment;

import org.springframework.boot.SpringApplication;

public class TestInvestmentApplication {

	public static void main(String[] args) {
		SpringApplication.from(InvestmentApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
