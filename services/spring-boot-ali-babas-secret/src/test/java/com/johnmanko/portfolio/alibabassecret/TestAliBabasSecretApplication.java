package com.johnmanko.portfolio.alibabassecret;

import org.springframework.boot.SpringApplication;

public class TestAliBabasSecretApplication {

	public static void main(String[] args) {
		SpringApplication.from(AliBabasSecretApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
