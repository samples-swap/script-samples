package com.operative.pipelinetracker.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

public class SetKafkaConsumergroupOffset {

  public static void main(String[] args) {
    String consumerGroupId = "consumerGroupId";
    String kafkaHost = "localhost:8091";
    Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

    long targetOffset = 100;
    OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(targetOffset);
    TopicPartition topicPartition_0 = new TopicPartition("mytopic", 0);
    TopicPartition topicPartition_1 = new TopicPartition("mytopic", 1);
    offsets.put(topicPartition_0, offsetAndMetadata);
    offsets.put(topicPartition_1, offsetAndMetadata);

    changeOffset(consumerGroupId, kafkaHost, offsets);
    
  }

  private static void changeOffset(String consumerGroupId, String kafkaHost,
      Map<TopicPartition, OffsetAndMetadata> offsets) {
    KafkaConsumer<String, String> consumer = getKafkaConsumer(consumerGroupId, kafkaHost);
    consumer.commitSync(offsets);

  }

  private static KafkaConsumer<String, String> getKafkaConsumer(String consumerGroupId, String kafkaHost) {
    Properties props = new Properties();

    props.put("bootstrap.servers", kafkaHost);
    props.put("group.id", consumerGroupId);
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("enable.auto.commit", true);
    props.put("value.deserializer", StringDeserializer.class.getName());
    props.put("session.timeout.ms", "30000");
    props.put("auto.offset.reset", "earliest");
    return new KafkaConsumer<>(props);
  }
