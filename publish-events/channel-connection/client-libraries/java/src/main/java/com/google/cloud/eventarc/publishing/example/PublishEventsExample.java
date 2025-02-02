/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// [START eventarc_third_party_publish_java]
package com.google.cloud.eventarc.publishing.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.eventarc.publishing.v1.PublishChannelConnectionEventsRequest;
import com.google.cloud.eventarc.publishing.v1.PublishChannelConnectionEventsResponse;
import com.google.cloud.eventarc.publishing.v1.PublisherClient;
import com.google.protobuf.Any;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.core.v1.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.protobuf.ProtobufFormat;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PublishEventsExample {

  static Logger LOGGER = Logger.getLogger(PublishEventsExample.class.getName());
  // Controls the way of sending events to Eventarc. 'true' for using text format
  // 'false' for proto format.
  static final boolean useTextEvent = false;

  /**
   * CustomMessage represents a payload delivered as a content of the CloudEvent.
   */
  class CustomMessage {
    public CustomMessage(String message) {
      this.message = message;
    }

    public String message;
  }

  private void SendEventUsingTextFormat(String channelConnectionName, CloudEvent event) {
    byte[] serializedEvent = EventFormatProvider.getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE)
        .serialize(event);
    String stringizedEvent = new String(serializedEvent);

    PublishChannelConnectionEventsRequest request = PublishChannelConnectionEventsRequest.newBuilder()
        .setChannelConnection(channelConnectionName)
        .addTextEvents(stringizedEvent)
        .build();
    LOGGER.log(Level.INFO, "Publishing message in Eventarc");
    try {
      // Create a client with credentials provided by the system.
      PublisherClient client = PublisherClient.create();
      PublishChannelConnectionEventsResponse response = client.publishChannelConnectionEvents(request);
      LOGGER.log(Level.INFO, String.format("Message published successfully.\nReceived response: %s",
          response.toString()));
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "An exception occurred while publishing", ex);
    }
  }

  private void SendEventUsingProtoFormat(String channelConnectionName, CloudEvent event) throws Exception {
    byte[] serializedEvent = EventFormatProvider.getInstance()
        .resolveFormat(ProtobufFormat.PROTO_CONTENT_TYPE)
        .serialize(event);
    io.cloudevents.v1.proto.CloudEvent protoEvent = io.cloudevents.v1.proto.CloudEvent.parseFrom(serializedEvent);
    Any wrappedEvent = Any.pack(protoEvent);

    PublishChannelConnectionEventsRequest request = PublishChannelConnectionEventsRequest.newBuilder()
        .setChannelConnection(channelConnectionName)
        .addEvents(wrappedEvent)
        .build();
    LOGGER.log(Level.INFO, "Publishing message in Eventarc");
    try {
      // Create a client with credentials provided by the system.
      PublisherClient client = PublisherClient.create();
      PublishChannelConnectionEventsResponse response = client.publishChannelConnectionEvents(request);
      LOGGER.log(Level.INFO, String.format("Message published successfully.\nReceived response: %s",
          response.toString()));
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "An exception occurred while publishing", ex);
    }
  }

  public void SendPublishEvent(String projectId, String region, String channelConnection) throws Exception {

    CustomMessage message = new CustomMessage("Hello world from Java client library");

    LOGGER.log(Level.INFO, "Building CloudEvent");

    ObjectMapper objectMapper = new ObjectMapper();
    CloudEvent event = new CloudEventBuilder()
        .withId(UUID.randomUUID().toString())
        .withSource(URI.create("//provider/source"))
        .withType("provider.v1.event")
        .withTime(OffsetDateTime.now())
        .withExtension("extsourcelang", "java")
        .withData("application/json",
            JsonCloudEventData.wrap(objectMapper.valueToTree(message)))
        .build();

    String channelName = "projects/" + projectId + "/locations/" + region + "/channelConnections/" + channelConnection;

    if (useTextEvent) {
      SendEventUsingTextFormat(channelName, event);
    } else {
      SendEventUsingProtoFormat(channelName, event);
    }
  }

  public static void main(String[] args) throws Exception {
    String projectId = args[0];
    String region = args[1];
    String channelConnection = args[2];
    System.out.println("ProjectId: " + projectId + " Region: " + region + " ChannelConnection: " + channelConnection);

    new PublishEventsExample().SendPublishEvent(projectId, region, channelConnection);
  }
}
// [END eventarc_third_party_publish_java]
