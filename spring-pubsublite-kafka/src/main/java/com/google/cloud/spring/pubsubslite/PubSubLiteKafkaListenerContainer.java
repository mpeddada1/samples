package com.google.cloud.spring.pubsubslite;

import com.google.cloud.pubsublite.CloudRegion;
import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.ProjectNumber;
import com.google.cloud.pubsublite.SubscriptionName;
import com.google.cloud.pubsublite.SubscriptionPath;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.cloudpubsub.FlowControlSettings;
import com.google.cloud.pubsublite.kafka.ConsumerSettings;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

public class PubSubLiteKafkaListenerContainer implements MessageListenerContainer {

	PubSubLiteKafkaListener listener;
	private long projectId;
	private String topicName;
	private String region;
	private char zone;
	private String subscriptionName;

	// But instead of doing this we can implement LiteConsumerFactory implements ConsumerFactory
	// to create a Consumer by calling ConsumerSettings.instantiiate()
	public PubSubLiteKafkaListenerContainer(long projectId, String topicName, String region, char zone, String subscriptionName) {

		this.projectId = projectId;
		this.topicName = topicName;
		this.region = region;
		this.zone = zone;
		this.subscriptionName = subscriptionName;
	}

	@Override
	public void setupMessageListener(Object messageListener) {
		this.listener = (PubSubLiteKafkaListener) messageListener;
	}

	@Override
	public Map<String, Map<MetricName, ? extends Metric>> metrics() {
		return null;
	}

	@Override
	public void start() {
		CloudZone location = CloudZone.of(CloudRegion.of(region), zone);

		TopicPath topicPath =
				TopicPath.newBuilder()
						.setLocation(location)
						.setProject(ProjectNumber.of(projectId))
						.setName(TopicName.of(topicName))
						.build();

		SubscriptionPath subscription =
				SubscriptionPath.newBuilder()
						.setLocation(location)
						.setProject(ProjectNumber.of(projectId))
						.setName(SubscriptionName.of(subscriptionName))
						.build();

		FlowControlSettings flowControlSettings =
				FlowControlSettings.builder()
						// 50 MiB. Must be greater than the allowed size of the largest message (1 MiB).
						.setBytesOutstanding(50 * 1024 * 1024L)
						// 10,000 outstanding messages. Must be >0.
						.setMessagesOutstanding(10000L)
						.build();

		ConsumerSettings settings =
				ConsumerSettings.newBuilder()
						.setSubscriptionPath(subscription)
						.setPerPartitionFlowControlSettings(flowControlSettings)
						.setAutocommit(true)
						.build();

		try (Consumer<byte[], byte[]> consumer = settings.instantiate()) {
			consumer.subscribe(Arrays.asList(topicPath.toString()));
			while (true) {
				ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(100));
				for (ConsumerRecord<byte[], byte[]> record : records) {
					listener.onMessage(record);
				}
			}
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isRunning() {
		return false;
	}
}
