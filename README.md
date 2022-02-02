# samples

To experiment with sample apps.
In order to experiment with these modules, we first need to manually create a topic and subscription, following the instructions in the [official documentation](https://cloud.google.com/pubsub/lite/docs/topics).  The samples below only experiment with sending and receiving messages from the Pub/Sub Lite topics and subscriptions. 

### Kafka to Pub/Sub Lite Migration through Spring
The `spring-pubsublite-kafka` contains samples exemplify a workflow that allows users to port their Kafka applications to Pub/Sub Lite with the help of Spring as the fascade. They leverage the Pub/Sub Lite-Kafka adapter in order to achieve this. 
It tests out the possibility of using **existing** Spring-Kafka modules such as KafkaTemplate and KafkaMessageListenerContainer to publish to Pub/Sub Lite. 

    - Dependendencies are:
    
      - [java-pubsublite-kafka](https://github.com/googleapis/java-pubsublite-kafka/tree/main/src/main/java/com/google/cloud/pubsublite/kafka)
      - [java-pubsublite](https://github.com/googleapis/java-pubsublite)
      - [spring-boot/spring-kafka](https://github.com/spring-projects/spring-kafka)
      - [spring-integration-kafka](https://github.com/spring-projects/spring-integration-kafka)
     
### Direct communication with Pub/Sub Lite through Spring
The `spring-pubsublite` module contains a sample for Spring support for using Pub/Sub Lite directly. The samples provide implementation of a Pub/Sub Lite template ( similar in theory to the PubSubTemplate which is currently provided by the Spring Cloud GCP project). It tests out sending and receiving messages to Pub/Sub Lite topics and subscriptions through the template. 

### Understanding the Pub/Sub Lite- Kafka Adapter
The `pubsub-lite-kafka` module just tests out the [Pub/Sub Lite Kafka Quickstart](https://cloud.google.com/pubsub/lite/docs/samples/pubsublite-kafka-consumer).

### Understanding Pub/Sub Lite through Cloud Libraries
The `pubsub-lite` module just tests out the [Pub/Sub Lite Quickstart](https://cloud.google.com/pubsub/lite/docs/samples/pubsublite-quickstart-publisher).
