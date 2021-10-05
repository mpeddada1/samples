package com.google.cloud.spring.pubsubslite;

import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.cloudpubsub.FlowControlSettings;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class KafkaLiteSpringIntegrationSample {
	public static void main(String[] args) throws Exception {
		try {
			ConfigurableApplicationContext context
					= new SpringApplicationBuilder(KafkaLiteSpringIntegrationSample.class)
					.web(WebApplicationType.NONE)
					.run(args);
			context.getBean(KafkaLiteSpringIntegrationSample.class).runDemo(context);
			context.close();
		}
		catch (Exception ex){
			ex.printStackTrace();
		}

	}

	String topicPath = "projects/931854040550/locations/us-central1-a/topics/my-second-topic";

	private void runDemo(ConfigurableApplicationContext context) {
		MessageChannel toKafka = context.getBean("toKafka", MessageChannel.class);
		System.out.println("Sending 10 messages...");
		String topicPath = 	this.topicPath;
		Map<String, Object> headers = new HashMap();
		for (int i = 0; i < 10; i++) {
			String payload = "foo-" + i;
			System.out.println("Sending message:" + payload);
			MessageHeaders messageHeaders = new MessageHeaders(headers);
			ProducerRecord record = new ProducerRecord(
				topicPath, "integration-demo".getBytes(), payload.getBytes());
			Message message = MessageBuilder.createMessage(record, messageHeaders);
			toKafka.send(message);
		}
		System.out.println("Sent messages.");

		// Receive Messages
		PollableChannel fromKafka = context.getBean("fromKafka", PollableChannel.class);
		Message<?> received = fromKafka.receive(10000);
		int count = 0;
		while (received != null) {
			System.out.println(received.getPayload());
			received = fromKafka.receive(++count < 11 ? 10000 : 1000);
		}
	}

	@Bean
	public ProducerFactory<?, ?> kafkaProducerFactory() {
		// Auto-configuration of properties can be used here to set things like batch settings.
		// We would need to initialize KafkaLiteProperties first and then pass it into LiteProducerFactory.
		LiteProducerFactory producerFactory = new LiteProducerFactory();
		TopicPath topicPath = TopicPath.parse("projects/931854040550/locations/us-central1-a/topics/my-second-topic");
		producerFactory.setTopicPath(topicPath);
		return producerFactory;
	}

	@ServiceActivator(inputChannel = "toKafka")
	@Bean
	public MessageHandler handler(KafkaTemplate<String, String> kafkaTemplate) {
		KafkaProducerMessageHandler<String, String> handler =
				new KafkaProducerMessageHandler<>(kafkaTemplate);
		LiteralExpression expression = new LiteralExpression(null);
		handler.setMessageKeyExpression(expression);
		return handler;
	}

	@Bean
	public ConsumerFactory<?, ?> kafkaConsumerFactory() {
		LiteConsumerFactory factory = new LiteConsumerFactory();
		factory.setTopicPath(this.topicPath);
		factory.setSubscriptionId("my-second-subscription");
		FlowControlSettings flowControlSettings =
				FlowControlSettings.builder()
						// 50 MiB. Must be greater than the allowed size of the largest message (1 MiB).
						.setBytesOutstanding(50 * 1024 * 1024L)
						// 10,000 outstanding messages. Must be >0.
						.setMessagesOutstanding(10000L)
						.build();
		factory.setFlowControlSettings(flowControlSettings);
		return factory;
	}

	@Bean
	public KafkaMessageListenerContainer<String, String> container(
			ConsumerFactory<String, String> kafkaConsumerFactory) {
		return new KafkaMessageListenerContainer<>(kafkaConsumerFactory,
				new ContainerProperties(this.topicPath));
	}


	@Bean
	public KafkaMessageDrivenChannelAdapter<String, String>
	adapter(KafkaMessageListenerContainer<String, String> container) {
		KafkaMessageDrivenChannelAdapter<String, String> kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter<>(container);
		kafkaMessageDrivenChannelAdapter.setOutputChannel(fromKafka());
		return kafkaMessageDrivenChannelAdapter;
	}


	@Bean
	public PollableChannel fromKafka() {
		return new QueueChannel();
	}


}

