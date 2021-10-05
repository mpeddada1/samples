package com.example.demo;

import com.google.cloud.spring.pubsubslite.KafkaPubSubLiteTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class PubSubLiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(PubSubLiteApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		String projectId = "mpeddada-test";
		String topicName = "projects/931854040550/locations/us-central1-a/topics/my-lite-topic-1";

		return args -> {
			publishMessage(topicName);
		};
	}

	@Autowired
	private KafkaPubSubLiteTemplate kafkaPubSubLiteTemplate;

	private void publishMessage(String topicName) throws ExecutionException, InterruptedException, IOException {
		kafkaPubSubLiteTemplate.send(topicName, "My first Pub/Sub Lite message!");
	}
}