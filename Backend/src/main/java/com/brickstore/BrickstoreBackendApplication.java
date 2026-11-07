package com.brickstore;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class BrickstoreBackendApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(BrickstoreBackendApplication.class, args);
	}

	private static void loadDotenv() {
		// Kiểm tra file .env ở thư mục hiện tại hoặc thư mục cha (gốc dự án)
		String envPath = "./";
		if (!new File(".env").exists() && new File("../.env").exists()) {
			envPath = "../";
		}

		Dotenv dotenv = Dotenv.configure()
				.directory(envPath)
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry -> {
			if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		});
	}
}
