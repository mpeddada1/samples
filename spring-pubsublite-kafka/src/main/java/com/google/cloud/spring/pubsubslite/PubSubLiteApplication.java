package com.google.cloud.spring.pubsubslite;

import com.google.cloud.pubsublite.TopicPath;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

//@SpringBootApplication
public class PubSubLiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(PubSubLiteApplication.class, args);
	}

 	@Autowired KafkaPubSubLite kafkaPubSubLite;

	@Bean
	public CommandLineRunner commandLineRunner() {
		String topicName = "projects/931854040550/locations/us-east1-b/topics/third-topic";

		return args -> {
			publishMessageWithDefaultSettings(topicName);
			//subscribeToMessage();
		};
	}

	private void publishMessageWithCustomTemplate(String topicName){
		String message = "Send message through custom template";
		kafkaPubSubLite.send(new ProducerRecord(topicName,  "demo".getBytes(), message.getBytes()));
	}


	private void publishMessageWithDefaultSettings(String topicName) {
		// Created KafkaTemplate with default settings
		LiteProducerFactory factory = new LiteProducerFactory();
		TopicPath topicPath = TopicPath.parse(topicName);
		factory.setTopicPath(topicPath);
		KafkaTemplate template = new KafkaTemplate(factory);
		for (int i =1; i< 10; i++){
			String message = "message-"+ i;
			System.out.println("Sending message: " + message);
			template.send(new ProducerRecord(topicName,  "demo".getBytes(), message.getBytes()));
		}
	}


	// Alternatively you can use the @KafkaListener annotation instead. You will need to create containerFactory bean first
	// (Create your own by implementing KafkaListenerContainerFactory) and register is as a bean.
	// Then call @KafkaListener(id=myListerer, containerFactory=myContainerFactory) public void listen(String mssg){System.out.println(message)};
	private void subscribeToMessage() {
		// currently this
		PubSubLiteKafkaListenerContainer listenerContainer = new PubSubLiteKafkaListenerContainer(
				931854040550L, "third-topic", "us-east1", 'b', "third-subscription");
		listenerContainer.setupMessageListener(new PubSubLiteKafkaListener());
		listenerContainer.start();
	}
}
