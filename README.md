# samples

To experiment with sample apps.

1. `spring-pubsublite-kafka` contains samples for Spring support for Pub/Sub Lite-Kafka adapter. It contains our own implementation of the template. It also tests out the possibility of using **existing** Spring-Kafka modules such as KafkaTemplate and KafkaMessageListenerContainer to publish to Pub/Sub Lite. 
    - Dependendencies are:
      - [java-pubsublite-kafka](https://github.com/googleapis/java-pubsublite-kafka/tree/main/src/main/java/com/google/cloud/pubsublite/kafka)
      - [java-pubsublite](https://github.com/googleapis/java-pubsublite)
      - [spring-boot/spring-kafka](https://github.com/spring-projects/spring-kafka)
      - [spring-integration-kafka](https://github.com/spring-projects/spring-integration-kafka)
2. `spring-pubsublite` contains a sample for Spring support for using Pub/Sub Lite directly. 
3. `pubsub-lite-kafka` just tests out the [Pub/Sub Lite Kafka Quickstart](https://cloud.google.com/pubsub/lite/docs/samples/pubsublite-kafka-consumer).
4. `pubsub-lite` just tests out the [Pub/Sub Lite Quickstart](https://cloud.google.com/pubsub/lite/docs/samples/pubsublite-quickstart-publisher).
