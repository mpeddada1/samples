package com.example.demo;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.pubsub.v1.ProjectSubscriptionName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


@SpringBootApplication
public class PubSubApplication {

	public static void main(String[] args) {
		SpringApplication.run(PubSubApplication.class, args);
	}

	@Autowired
	private PubSubTemplate pubSubTemplate;

	@Autowired
	Storage storage;


	@Bean
	public CommandLineRunner commandLineRunner() {
		String projectId = "mpeddada-test";
		String topicId = "my-topic";
		String subscriptionId = "my-sub";

		return args -> {
			publishMessage(topicId);
			receiveMessage(projectId, subscriptionId);
		};
	}

	private void publishMessage(String topicId) throws ExecutionException, InterruptedException, IOException {
		String message = "Hi!";
		ListenableFuture<String> messageIdFuture1 =  this.pubSubTemplate.publish(topicId, message);

		System.out.println("Published message: " + messageIdFuture1.get());

		ArrayList<String> messageLists = new ArrayList<>();
		messageLists.add(message);
		for (String m : messageLists){
			createFile(m, messageIdFuture1.get());
		}
	}

	private void receiveMessage(String projectId, String subscriptionId){
		ProjectSubscriptionName subscriptionName =
				ProjectSubscriptionName.of(projectId, subscriptionId);
		Subscriber subscriber = this.pubSubTemplate.subscribe(subscriptionName.toString(), message -> {
			System.out.println("Message received from " + subscriptionName + " subscription: "
					+ message.getPubsubMessage().getData().toStringUtf8());
			message.ack();
		});
		System.out.println(subscriber.getSubscriptionNameString());
		System.out.println(subscriber.getFlowControlSettings().getMaxOutstandingElementCount());
	}

	private void createFile(String message, String filename) throws IOException {
		storage.create(
				BlobInfo.newBuilder("pubsub_test_bucket1", filename).build(),
				message.getBytes()
		);
	}

}
