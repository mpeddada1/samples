package com.google.cloud.spring.pubsubslite;

import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.kafka.ProducerSettings;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LiteProducerFactory implements ProducerFactory {

	TopicPath topicPath;

	public void setTopicPath(TopicPath topicPath){
		this.topicPath = topicPath;

	}

	/**
	 * Return an unmodifiable reference to the configuration map for this factory. Will be called
	 * in KafkaProducerMessageHandler#determineSendTimeout. Implemented for Spring Integration. This
	 * can be autoconfigured.
	 */
	@Override
	public Map<String, Object> getConfigurationProperties() {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put("delivery.timeout.ms", null);
		return Collections.unmodifiableMap(configMap);
	}

	@Override
	public Producer createProducer() {
		ProducerSettings settings = ProducerSettings.newBuilder().setTopicPath(this.topicPath).build();
		return settings.instantiate();
}
}
