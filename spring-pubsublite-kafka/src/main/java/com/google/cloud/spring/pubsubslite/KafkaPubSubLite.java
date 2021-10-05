/*
 * Copyright 2015-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.pubsubslite;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.kafka.ProducerSettings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;


/**
 * This template is very similar to {@link org.springframework.kafka.core.KafkaTemplate} except that it just replaces
 * the use of the Kafka Producer with the Pub/Sub Lite Producer implementation.
 *
 */
public class KafkaPubSubLite<K, V> implements KafkaOperations<K, V>, ApplicationContextAware, BeanNameAware,
		ApplicationListener<ContextStoppedEvent>, DisposableBean {

	private static final UnsupportedVersionException NO_TRANSACTIONS_EXCEPTION =
			new UnsupportedVersionException(
					"Pub/Sub Lite is a non-transactional system and does not support producer transactions.");

	private RecordMessageConverter messageConverter = new MessagingMessageConverter();
	private final ProducerFactory<K, V> producerFactory;
	private String beanName = "kafkaTemplate";
	private ApplicationContext applicationContext;
	private final boolean customProducerFactory = true;
	private ConsumerFactory<K, V> consumerFactory;
	private String defaultTopic;

	/**
	 * Create an instance using the supplied producer factory and properties, with
	 * autoFlush false. If the configOverrides is not null or empty, a new
	 * {@link DefaultKafkaProducerFactory} will be created with merged producer properties
	 * with the overrides being applied after the supplied factory's properties.
	 * @param producerFactory the producer factory.
	 * @param configOverrides producer configuration properties to override.
	 * @since 2.5
	 */
	public KafkaPubSubLite(ProducerFactory<K, V> producerFactory, @Nullable Map<String, Object> configOverrides) {
		this.producerFactory = producerFactory;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (this.customProducerFactory) {
			((DefaultKafkaProducerFactory<K, V>) this.producerFactory).setApplicationContext(applicationContext);
		}
	}

	@Override
	// Not needed for Pub/Sub Lite
	public boolean isTransactional() {
		return false;
	}

	@Override
	// Not needed for Pub/Sub Lite
	public boolean isAllowNonTransactional() {
		return false;
	}

	/**
	 * Return the producer factory used by this template.
	 * @return the factory.
	 * @since 2.2.5
	 */
	@Override
	public ProducerFactory<K, V> getProducerFactory() {
		return this.producerFactory;
	}


	@Override
	public void onApplicationEvent(ContextStoppedEvent event) {
		if (this.customProducerFactory) {
			((DefaultKafkaProducerFactory<K, V>) this.producerFactory).onApplicationEvent(event);
		}
	}

	@Override
	public ListenableFuture<SendResult<K, V>> sendDefault(@Nullable V data) {
		return send(this.defaultTopic, data);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> sendDefault(K key, @Nullable V data) {
		return send(this.defaultTopic, key, data);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> sendDefault(Integer partition, K key, @Nullable V data) {
		return send(this.defaultTopic, partition, key, data);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> sendDefault(Integer partition, Long timestamp, K key, @Nullable V data) {
		return send(this.defaultTopic, partition, timestamp, key, data);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> send(String topic, @Nullable V data) {
		ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, data);
		return doSend((ProducerRecord<byte[], byte[]>) producerRecord);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> send(String s, K k, V v) {
		ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(s, k.toString().getBytes(), v.toString().getBytes());
		return doSend(producerRecord);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, K key, @Nullable V data) {
		ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, partition, key, data);
		return doSend((ProducerRecord<byte[], byte[]>) producerRecord);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, Long timestamp, K key,
												   @Nullable V data) {

		ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, partition, timestamp, key, data);
		return doSend((ProducerRecord<byte[], byte[]>) producerRecord);
	}

	@Override
	public ListenableFuture<SendResult<K, V>> send(ProducerRecord<K, V> record) {
		Assert.notNull(record, "'record' cannot be null");
		return doSend((ProducerRecord<byte[], byte[]>) record);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListenableFuture<SendResult<K, V>> send(Message<?> message) {
		ProducerRecord<?, ?> producerRecord = this.messageConverter.fromMessage(message, this.defaultTopic);
		if (!producerRecord.headers().iterator().hasNext()) { // possibly no Jackson
			byte[] correlationId = message.getHeaders().get(KafkaHeaders.CORRELATION_ID, byte[].class);
			if (correlationId != null) {
				producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId);
			}
		}
		return doSend((ProducerRecord<byte[], byte[]>) producerRecord);
	}

	/**
	 * Send the producer record.
	 * @param producerRecord the producer record.
	 * @return a Future for the {@link org.apache.kafka.clients.producer.RecordMetadata
	 * RecordMetadata}.
	 */
	protected ListenableFuture<SendResult<K, V>> doSend(final ProducerRecord<byte[], byte[]> producerRecord) {

		if (producerRecord.partition() != null) {
			throw new UnsupportedOperationException(
					"Pub/Sub Lite producers may not specify a partition in their records.");
		}

		// Parse TopicPath from topic string.
		TopicPath path = TopicPath.parse(producerRecord.topic());

		// This can be done through DefaultKafkaProducerFactory --> createProducer() which can return Producer from settings
		// same for consumer.
		ProducerSettings producerSettings = ProducerSettings.newBuilder().setTopicPath(path).build();
		try (Producer<byte[], byte[]> producer = producerSettings.instantiate()) {

			System.out.println("Sending: " + producerRecord);

			final SettableListenableFuture<SendResult<K, V>> future = new SettableListenableFuture<>();
			Future<RecordMetadata> sendFuture =
					producer.send(producerRecord);
			// May be an immediate failure
			try {
				sendFuture.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new KafkaException("Interrupted", e);
			} catch (ExecutionException e) {
				throw new KafkaException("Send failed", e.getCause()); // NOSONAR, stack trace
			}
			System.out.println("Sent: " + producerRecord);
			return future;
		}
	}

	@Override
	public List<PartitionInfo> partitionsFor(String topic) {
		// Assume 1 partition for now. Otherwise get partition count from admin.getTopicPartitionCount()
		final Node node = new Node(0, "pubsublite.googleapis.com", 443);
		final Node[] nodes = {node};
		return ImmutableList.of(new PartitionInfo(topic, 0, node, nodes, nodes));
	}

	@Override
	public Map<MetricName, ? extends Metric> metrics() {
		return ImmutableMap.of();
	}

	@Override
	public <T> T execute(ProducerCallback<K, V, T> callback) {
		throw NO_TRANSACTIONS_EXCEPTION;
	}

	@Override
	public <T> T executeInTransaction(OperationsCallback<K, V, T> callback) {
		throw NO_TRANSACTIONS_EXCEPTION;
	}

	/**
	 * {@inheritDoc}
	 * <p><b>Note</b> It only makes sense to invoke this method if the
	 * {@link ProducerFactory} serves up a singleton producer (such as the
	 * {@link DefaultKafkaProducerFactory}).
	 */
	@Override
	public void flush() {
		return;
	}


	@Override
	public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets) {
		throw NO_TRANSACTIONS_EXCEPTION;
	}

	@Override
	public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId) {
		throw NO_TRANSACTIONS_EXCEPTION;
	}

	@Override
	public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets,
										 ConsumerGroupMetadata groupMetadata) {

		throw NO_TRANSACTIONS_EXCEPTION;
	}


//	@Override
//	@Nullable
//	public ConsumerRecord<K, V> receive(String topic, int partition, long offset) {
//		return receive(topic, partition, offset, DEFAULT_POLL_TIMEOUT);
//	}
//
//	@Override
//	@Nullable
//	public ConsumerRecord<K, V> receive(String topic, int partition, long offset, Duration pollTimeout) {
//		Assert.notNull(this.consumerFactory, "A consumerFactory is required");
//		Properties props = new Properties();
//		props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
//		try (Consumer<K, V> consumer = this.consumerFactory.createConsumer(null, null, null, props)) {
//			TopicPartition topicPartition = new TopicPartition(topic, partition);
//			consumer.assign(Collections.singletonList(topicPartition));
//			consumer.seek(topicPartition, offset);
//			ConsumerRecords<K, V> records = consumer.poll(pollTimeout);
//			if (records.count() == 1) {
//				return records.iterator().next();
//			}
//			return null;
//		}
//	}

	/**
	 * Return true if the template is currently running in a transaction on the calling
	 * thread.
	 * @return true if a transaction is running.
	 * @since 2.2.1
	 */
	@Override
	public boolean inTransaction() {
		return false;
	}

	@Override
	public void destroy() {
		if (this.customProducerFactory) {
			((DefaultKafkaProducerFactory<K, V>) this.producerFactory).destroy();
		}
	}
}
