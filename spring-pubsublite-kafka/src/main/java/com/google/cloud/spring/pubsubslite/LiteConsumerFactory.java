package com.google.cloud.spring.pubsubslite;

import com.google.cloud.pubsublite.CloudRegion;
import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.ProjectNumber;
import com.google.cloud.pubsublite.SubscriptionName;
import com.google.cloud.pubsublite.SubscriptionPath;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.cloudpubsub.FlowControlSettings;
import com.google.cloud.pubsublite.kafka.ConsumerSettings;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.HashMap;
import java.util.Map;

public class LiteConsumerFactory implements ConsumerFactory {

	String topicPath;
	String subscriptionId;
	FlowControlSettings flowControlSettings;

	public void setTopicPath(String topicPath){
		this.topicPath = topicPath;
	}

	// Introduce method to pass in full subscription path instead to verify that subscription region and zone are
	// correct and matching those of the topic.
	public void setSubscriptionId(String subscriptionId){
		this.subscriptionId = subscriptionId;
	}

	public void setFlowControlSettings(FlowControlSettings flowControlSettings){
		this.flowControlSettings = flowControlSettings;
	}

	@Override
	public Consumer createConsumer(String groupId, String clientIdPrefix, String clientIdSuffix) {
		TopicPath topicPath = TopicPath.parse(this.topicPath);
		String cloudRegion = topicPath.location().extractRegion().toString();
		char zoneId = topicPath.location().zone().zoneId();
		Long projectNumber = topicPath.project().number().value();

		CloudZone location = CloudZone.of(CloudRegion.of(cloudRegion), zoneId);


		SubscriptionPath subscription =
				SubscriptionPath.newBuilder()
						.setLocation(location)
						.setProject(ProjectNumber.of(projectNumber))
						.setName(SubscriptionName.of(subscriptionId))
						.build();

		ConsumerSettings settings =
				ConsumerSettings.newBuilder()
						.setSubscriptionPath(subscription)
						.setPerPartitionFlowControlSettings(this.flowControlSettings)
						.setAutocommit(true)
						.build();
		return settings.instantiate();
	}

	@Override
	public boolean isAutoCommit() {
		return false;
	}

	/**
	 * Return an unmodifiable reference to the configuration map for this factory.
	 * Useful for cloning to make a similar factory. To be valled in AbstractMessageListenerContainer#checkGroupId.
	 * @return the configs.
	 * @since 2.0
	 */
	@Override
	public Map<String, Object> getConfigurationProperties() {
		HashMap<String, Object> configMap = new HashMap<>();
		configMap.put(ConsumerConfig.GROUP_ID_CONFIG, "groupId");
		return configMap;
	}
}
