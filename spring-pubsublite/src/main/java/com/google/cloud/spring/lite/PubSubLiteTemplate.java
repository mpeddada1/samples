/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.lite;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.SubscriberInterface;
import com.google.cloud.pubsublite.MessageMetadata;
import com.google.cloud.pubsublite.SubscriptionPath;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.cloudpubsub.FlowControlSettings;
import com.google.cloud.pubsublite.cloudpubsub.MessageTransforms;
import com.google.cloud.pubsublite.cloudpubsub.Publisher;
import com.google.cloud.pubsublite.cloudpubsub.PublisherSettings;
import com.google.cloud.pubsublite.cloudpubsub.Subscriber;
import com.google.cloud.pubsublite.cloudpubsub.SubscriberSettings;
import com.google.cloud.spring.pubsub.core.PubSubDeliveryException;
import com.google.cloud.spring.pubsub.core.PubSubOperations;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import com.google.cloud.spring.pubsub.support.converter.SimplePubSubMessageConverter;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Component
public class PubSubLiteTemplate implements PubSubOperations {

	private PubSubMessageConverter pubSubMessageConverter = new SimplePubSubMessageConverter();

	public PubSubLiteTemplate() {

	}

	@Override
	// Need full path for topic id
	public <T> ListenableFuture<String> publish(String topic, T payload, Map<String, String> headers) {
		return null;
	}

	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload) {
		TopicPath topicPath = TopicPath.parse(topic);

		// This would have to be done in DefaultLitePublisherFactory
		PublisherSettings publisherSettings = PublisherSettings.newBuilder().setTopicPath(topicPath).build();
		Publisher publisher = Publisher.create(publisherSettings);

		// Start the publisher. Upon successful starting, its state will become RUNNING.
		publisher.startAsync().awaitRunning();

		// Ordering key can be set using gcp_pubsub_ordering_key= blah
		PubsubMessage pubsubMessage = this.pubSubMessageConverter.toPubSubMessage(payload, (Map) null);
		ApiFuture<String> publishFuture = publisher.publish(pubsubMessage);

		// Logging based on whether publishing failed or succeeded
		final SettableListenableFuture<String> settableFuture = new SettableListenableFuture();
		ApiFutures.addCallback(publishFuture, new ApiFutureCallback<String>() {
			public void onFailure(Throwable throwable) {
				String errorMessage = "Publishing to " + topic + " topic failed.";
				System.out.println(errorMessage);
				PubSubDeliveryException pubSubDeliveryException = new PubSubDeliveryException(pubsubMessage, errorMessage, throwable);
				settableFuture.setException(pubSubDeliveryException);
			}

			public void onSuccess(String result) {
				System.out.println("Publishing to " + topic + " was successful. Message ID: " + result);

				settableFuture.set(result);
			}
		}, MoreExecutors.directExecutor());
		return settableFuture;
	}

	@Override
	public ListenableFuture<String> publish(String topic, com.google.pubsub.v1.PubsubMessage pubsubMessage) {
		return null;
	}

	@Override
	public com.google.cloud.pubsub.v1.Subscriber subscribe(String subscription, Consumer<BasicAcknowledgeablePubsubMessage> messageConsumer) {
		SubscriptionPath subscriptionPath = SubscriptionPath.parse(subscription);

		// Need to spring messages in MessageMetadata format -> including partitions etc.
		MessageReceiver receiver =
				(PubsubMessage message, AckReplyConsumer consumer) -> {
					System.out.println("Id : " + MessageMetadata.decode(message.getMessageId()));
					System.out.println("Data : " + message.getData().toStringUtf8());
					System.out.println("Ordering key : " + message.getOrderingKey());
					System.out.println("Attributes : ");
					consumer.ack();
				};


		// Flow control settings are REQUIRED
		FlowControlSettings flowControlSettings =
				FlowControlSettings.builder()
						// 10 MiB. Must be greater than the allowed size of the largest message (1 MiB).
						.setBytesOutstanding(10 * 1024 * 1024L)
						// 1,000 outstanding messages. Must be >0.
						.setMessagesOutstanding(1000L)
						.build();

		SubscriberSettings subscriberSettings =
				SubscriberSettings.newBuilder()
						.setSubscriptionPath(subscriptionPath)
						.setPerPartitionFlowControlSettings(flowControlSettings)
						.setReceiver(receiver).build();
		Subscriber subscriber = Subscriber.create(subscriberSettings);

		SubscriberInterface subscriberInterface = (SubscriberInterface)subscriber;
		// Start the subscriber. Upon successful starting, its state will become RUNNING.
		subscriberInterface.startAsync().awaitRunning();
		System.out.println("Listening to messages on " + subscriptionPath.toString() + "...");

		try {
			System.out.println(subscriber.state());
			// Wait 90 seconds for the subscriber to reach TERMINATED state. If it encounters
			// unrecoverable errors before then, its state will change to FAILED and an
			// IllegalStateException will be thrown.
			subscriber.awaitTerminated(90, TimeUnit.SECONDS);
		} catch (TimeoutException t) {
			// Shut down the subscriber. This will change the state of the subscriber to TERMINATED.
			subscriber.stopAsync().awaitTerminated();
			System.out.println("Subscriber is shut down: " + subscriber.state());
		}

		// Leaving this in temporarily to appease compiler
		return (com.google.cloud.pubsub.v1.Subscriber) subscriberInterface;
}

	@Override
	public <T> com.google.cloud.pubsub.v1.Subscriber subscribeAndConvert(String subscription, Consumer<ConvertedBasicAcknowledgeablePubsubMessage<T>> convertedBasicAcknowledgeablePubsubMessageConsumer, Class<T> payloadType) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages, Boolean returnImmediately) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public ListenableFuture<List<PubsubMessage>> pullAndAckAsync(String subscription, Integer maxMessages, Boolean returnImmediately) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages, Boolean returnImmediately) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public org.springframework.util.concurrent.ListenableFuture<List<AcknowledgeablePubsubMessage>> pullAsync(String subscription, Integer maxMessages, Boolean returnImmediately) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public <T> List<ConvertedAcknowledgeablePubsubMessage<T>> pullAndConvert(String subscription, Integer maxMessages, Boolean returnImmediately, Class<T> payloadType) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public <T> ListenableFuture<List<ConvertedAcknowledgeablePubsubMessage<T>>> pullAndConvertAsync(String subscription, Integer maxMessages, Boolean returnImmediately, Class<T> payloadType) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public PubsubMessage pullNext(String subscription) {
		return null;
	}

	@Override
	// Not needed for pub/sub lite
	public ListenableFuture<PubsubMessage> pullNextAsync(String subscription) {
		return null;
	}

	@Override
	public ListenableFuture<Void> ack(Collection<? extends AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		return null;
	}

	@Override
	public ListenableFuture<Void> nack(Collection<? extends AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		return null;
	}

	@Override
	public ListenableFuture<Void> modifyAckDeadline(Collection<? extends AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages, int ackDeadlineSeconds) {
		return null;
	}
}
