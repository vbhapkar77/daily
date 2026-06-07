package com.vishalbhapkar.daily;

import org.springframework.boot.SpringApplication;

public class TestDailyApplication {

	public static void main(String[] args) {
		SpringApplication.from(DailyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
