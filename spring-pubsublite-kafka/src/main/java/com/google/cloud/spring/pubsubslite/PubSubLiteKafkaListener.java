package com.google.cloud.spring.pubsubslite;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;

import java.util.Base64;

public class PubSubLiteKafkaListener implements MessageListener {
	@Override
	//To process data coming from poll()
	public void onMessage(Object data) {
		long offset = ((ConsumerRecord<byte[], byte[]>)data).offset();
		String value = Base64.getEncoder().encodeToString(((ConsumerRecord<byte[], byte[]>)data).value());

		System.out.printf("Received %s: %s%n", offset, value);
	}
}
