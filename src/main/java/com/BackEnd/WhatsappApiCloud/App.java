package com.BackEnd.WhatsappApiCloud;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Guayaquil"));
		SpringApplication.run(App.class, args);
	}

}
