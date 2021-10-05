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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class SpringPubSubLiteApplication {

	public static void main(String[] args) {
			SpringApplication.run(SpringPubSubLiteApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		String projectId = "mpeddada-test";
		String topicName = "projects/931854040550/locations/us-east1-b/topics/third-topic";
		String subscriptionName = "projects/931854040550/locations/us-east1-b/subscriptions/third-subscription";

		return args -> {
			publishMessage(topicName);
			subscribe(subscriptionName);
		};
	}

	@Autowired
	private PubSubLiteTemplate pubSubLiteTemplate;

	private void publishMessage(String topicName) {
		for (int i = 0; i < 5; i++) {
			String message = "message-" + i;
			pubSubLiteTemplate.publish(topicName, message);
		}
	}

	private void subscribe(String subscriptionName) {
		pubSubLiteTemplate.subscribe(subscriptionName, null);
	}


}
