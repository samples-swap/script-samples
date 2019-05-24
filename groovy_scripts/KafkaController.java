package com.gosuncn.godeyes.kafka.controller;

import com.gosuncn.godeyes.common.RestResult;
import com.gosuncn.godeyes.kafka.entity.GroupInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/kafka", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
public class KafkaController {

    private static final  String MONITOR_GROUP_ID = "monitoring_consumer_" + UUID.randomUUID().toString();

    private static KafkaConsumer<?, ?> createNewConsumer(String groupId, String host) {
        Properties properties = getProperties(groupId, host);
        return new KafkaConsumer<>(properties);
    }

    private static Properties getProperties(String groupId, String host) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    @PostMapping("/offsets")
    public ResponseEntity<RestResult> offsets(@RequestBody List<GroupInfo> groupInfoList) {

        log.info("recv: {}", groupInfoList);

        // 计算每个消费组里边的各个topic消息堆积情况
        groupInfoList.forEach(groupInfo -> {
            groupInfo.getData().forEach((row) -> {
                log.info("groupId: {}, topic: {}, partition: {}, offset: {}, total: {}, clientId: {}, consumerId: {}, hostId: {}", groupInfo.getGroup(), row.getTopic(), row.getPartition(), row.getOffset(), row.getTotal(), row.getClientId(), row.getConsumerId(), row.getHost());
            });
        });

        return ResponseEntity.ok(RestResult.OK_EMPTY_MSG);
    }

    @GetMapping("/print")
    public ResponseEntity<RestResult> print() throws ExecutionException, InterruptedException {

        String host = "10.94.44.74:9092,10.94.44.115:9092,10.94.44.76:9092";

        KafkaAdminClient client = (KafkaAdminClient) KafkaAdminClient.create(getProperties(MONITOR_GROUP_ID, host));

        KafkaConsumer<?, ?> consumer = createNewConsumer(MONITOR_GROUP_ID, host);

        try {

            List<String> groupIds = client.listConsumerGroups().all().get().stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());

            Map<String, ConsumerGroupDescription> groupDescriptionMap = client.describeConsumerGroups(groupIds).all().get();

            for(String groupId : groupIds) {

                ConsumerGroupDescription consumerGroupDescription = groupDescriptionMap.get(groupId);

                Map<TopicPartition, OffsetAndMetadata> groupOffsets = client.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();

                consumer.endOffsets(groupOffsets.keySet()).forEach((tp, total) -> {

                    MemberDescription md = null;
                    for(MemberDescription memberDescription: consumerGroupDescription.members()) {
                        if(memberDescription.assignment().topicPartitions().contains(tp)) {
                            md = memberDescription;
                            break;
                        }
                    }
                    OffsetAndMetadata offsetAndMetadata = groupOffsets.get(tp);

                    String clientId = "";
                    String consumerId = "";
                    String hostId = "";

                    if(md != null) {
                        clientId = md.clientId();
                        consumerId = md.consumerId();
                        hostId = md.host();
                    }

                    log.info("groupId: {}, topic: {}, partition: {}, offset: {}, total: {}, clientId: {}, consumerId: {}, hostId: {}", groupId, tp.topic(), tp.partition(), offsetAndMetadata.offset(), total, clientId, consumerId, hostId);
                });
            }
        } finally {
            client.close();
            consumer.close();
        }

        return ResponseEntity.ok(RestResult.OK_EMPTY_MSG);
    }
}