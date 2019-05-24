
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Kafka的AdminClient函式庫，支持管理和檢查topics, brokers, configurations和ACLs。
 * 所需的最小的Kafka broker版本為0.10.0.0。有些API會需要更高版本的Kafka broker的話會註解在API中。
 *
 * == 使用Kafka的AdminClient API來查詢Kafka中特定ConsumerGroup的offset ==
 */
public class ds01_AdminClient_describe_cg_info2 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 步驟1. 設定要連線到Kafka集群的相關設定
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Kafka集群在那裡?

        // 步驟2. 創建AdminClient的instance
        AdminClient adminClient = KafkaAdminClient.create(props); // 透過create()來產生adminClient的instance

        // 步驟3. 透過AdminClient的API來取得相關ConsumerGroup的訊息
        // *** 取得Kafka叢集裡ConsumerGroup基本資訊 ***  //
        ListConsumerGroupsResult listConsumerGroupsResult = adminClient.listConsumerGroups();

        // 步驟4. 指定想要查找的ConsumerGroup
        String consumerGroupId = "xxxxx"; // <-- 替換你/妳的ConsumerGroup ID
        System.out.println("ConsumerGroup: " + consumerGroupId);

        ListConsumerGroupOffsetsResult listConsumerGroupOffsetsResult = adminClient.listConsumerGroupOffsets(consumerGroupId);
        // 取得這個ConsumerGroup曾經訂閱過的Topics的最後offsets
        Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = listConsumerGroupOffsetsResult.partitionsToOffsetAndMetadata().get();

        // 我們產生一個這個ConsumerGroup曾經訂閱過的TopicParition訊息
        List<TopicPartition> topicPartitions = new ArrayList<>();
        for(Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsetAndMetadataMap.entrySet()) {
            TopicPartition topic_partition = entry.getKey(); // 某一個topic的某一個partition
            OffsetAndMetadata offset = entry.getValue(); // offset

            // 打印出來 (在API裡頭取到的offset都是那個partition最大的offset+1 (也就是下一個訊息會被assign的offset),
            // 因此我們減1來表示現在己經消費過的最大offset
            System.out.println(String.format(" Topic: %s Partiton: %d Offset: %d", topic_partition.topic(), topic_partition.partition(), offset.offset()));
            topicPartitions.add(topic_partition);
        }

        // 步驟5. 適當地釋放AdminClient的資源
        adminClient.close();
    }