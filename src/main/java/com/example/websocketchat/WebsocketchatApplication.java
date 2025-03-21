package com.example.websocketchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebsocketchatApplication {
	public static void main(String[] args) {
		SpringApplication.run(WebsocketchatApplication.class, args);
	}
}
