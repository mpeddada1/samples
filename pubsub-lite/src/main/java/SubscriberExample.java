import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsublite.CloudRegion;
import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.MessageMetadata;
import com.google.cloud.pubsublite.ProjectNumber;
import com.google.cloud.pubsublite.SubscriptionName;
import com.google.cloud.pubsublite.SubscriptionPath;
import com.google.cloud.pubsublite.cloudpubsub.FlowControlSettings;
import com.google.cloud.pubsublite.cloudpubsub.MessageTransforms;
import com.google.cloud.pubsublite.cloudpubsub.Subscriber;
import com.google.cloud.pubsublite.cloudpubsub.SubscriberSettings;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SubscriberExample {

	public static void main(String... args) throws Exception {
		// TODO(developer): Replace these variables before running the sample.
		String cloudRegion = "us-central1";
		char zoneId = 'a';
		// Choose an existing subscription for the subscribe example to work.
		String subscriptionId = "your-lite-subscription";
		long projectNumber = Long.parseLong("931854040550");

		subscriberExample(cloudRegion, zoneId, projectNumber, subscriptionId);
	}

	public static void subscriberExample(
			String cloudRegion, char zoneId, long projectNumber, String subscriptionId)
			throws ApiException {

		SubscriptionPath subscriptionPath =
				SubscriptionPath.newBuilder()
						.setLocation(CloudZone.of(CloudRegion.of(cloudRegion), zoneId))
						.setProject(ProjectNumber.of(projectNumber))
						.setName(SubscriptionName.of(subscriptionId))
						.build();

		// The message stream is paused based on the maximum size or number of messages that the
		// subscriber has already received, whichever condition is met first.
		FlowControlSettings flowControlSettings =
				FlowControlSettings.builder()
						// 10 MiB. Must be greater than the allowed size of the largest message (1 MiB).
						.setBytesOutstanding(10 * 1024 * 1024L)
						// 1,000 outstanding messages. Must be >0.
						.setMessagesOutstanding(1000L)
						.build();

		MessageReceiver receiver =
				(PubsubMessage message, AckReplyConsumer consumer) -> {
					System.out.println("Id : " + MessageMetadata.decode(message.getMessageId()));
					System.out.println("Data : " + message.getData().toStringUtf8());
					System.out.println("Ordering key : " + message.getOrderingKey());
					System.out.println("Attributes : ");
					message
							.getAttributesMap()
							.forEach(
									(key, value) -> {
										if (key == MessageTransforms.PUBSUB_LITE_EVENT_TIME_TIMESTAMP_PROTO) {
											Timestamp ts = MessageTransforms.decodeAttributeEventTime(value);
											System.out.println(key + " = " + ts.toString());
										} else {
											System.out.println(key + " = " + value);
										}
									});

					// Acknowledge the message.
					consumer.ack();
				};

		SubscriberSettings subscriberSettings =
				SubscriberSettings.newBuilder()
						.setSubscriptionPath(subscriptionPath)
						.setReceiver(receiver)
						// Flow control settings are set at the partition level.
						.setPerPartitionFlowControlSettings(flowControlSettings)
						.build();

		Subscriber subscriber = Subscriber.create(subscriberSettings);

		// Start the subscriber. Upon successful starting, its state will become RUNNING.
		subscriber.startAsync().awaitRunning();

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
	}
}