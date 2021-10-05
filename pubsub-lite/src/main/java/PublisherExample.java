import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsublite.CloudRegion;
import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.MessageMetadata;
import com.google.cloud.pubsublite.ProjectNumber;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.cloudpubsub.Publisher;
import com.google.cloud.pubsublite.cloudpubsub.PublisherSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PublisherExample {

	public static void main(String... args) throws Exception {
		// TODO(developer): Replace these variables before running the sample.
		String cloudRegion = "us-central1";
		char zoneId = 'a';
		// Choose an existing topic for the publish example to work.
		String topicId = "your-lite-topic";
		long projectNumber = Long.parseLong("931854040550");
		int messageCount = 100;

		publisherExample(cloudRegion, zoneId, projectNumber, topicId, messageCount);
	}

	// Publish messages to a topic.
	public static void publisherExample(
			String cloudRegion, char zoneId, long projectNumber, String topicId, int messageCount)
			throws ApiException, ExecutionException, InterruptedException {

		TopicPath topicPath =
				TopicPath.newBuilder()
						.setProject(ProjectNumber.of(projectNumber))
						.setLocation(CloudZone.of(CloudRegion.of(cloudRegion), zoneId))
						.setName(TopicName.of(topicId))
						.build();
		Publisher publisher = null;
		List<ApiFuture<String>> futures = new ArrayList<>();

		try {
			PublisherSettings publisherSettings =
					PublisherSettings.newBuilder().setTopicPath(topicPath).build();

			publisher = Publisher.create(publisherSettings);

			// Start the publisher. Upon successful starting, its state will become RUNNING.
			publisher.startAsync().awaitRunning();

			for (int i = 0; i < messageCount; i++) {
				String message = "message-" + i;

				// Convert the message to a byte string.
				ByteString data = ByteString.copyFromUtf8(message);
				PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

				// Publish a message. Messages are automatically batched.
				ApiFuture<String> future = publisher.publish(pubsubMessage);
				futures.add(future);
			}
		} finally {
			ArrayList<MessageMetadata> metadata = new ArrayList<>();
			List<String> ackIds = ApiFutures.allAsList(futures).get();
			for (String id : ackIds) {
				// Decoded metadata contains partition and offset.
				metadata.add(MessageMetadata.decode(id));
			}
			System.out.println(metadata + "\nPublished " + ackIds.size() + " messages.");

			if (publisher != null) {
				// Shut down the publisher.
				publisher.stopAsync().awaitTerminated();
				System.out.println("Publisher is shut down.");
			}
		}
	}
}
