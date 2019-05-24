package com.lxy.kafka.demo.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        factory.getContainerProperties().setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {

                Map<TopicPartition, Long> timestampsToSearch = new HashMap<>(partitions.size());

                // 2018-11-23 16:30:10
                for (TopicPartition partition : partitions) {
                    timestampsToSearch.put(partition,  1543395046000L);
                }

                Map<TopicPartition, OffsetAndTimestamp> outOffsets = consumer.offsetsForTimes(timestampsToSearch);

                for (TopicPartition partition : partitions) {

                    OffsetAndTimestamp ot = outOffsets.get(partition);

                    if(ot != null) {
                        long seekOffset = ot.offset();
//                        Long currentPosition = consumer.position(partition);
                        consumer.seek(partition, seekOffset);
                    }

                }
            }
        });

        return factory;
    }
}