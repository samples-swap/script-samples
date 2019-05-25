
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Kafka的AdminClient函式庫，支持管理和檢查topics, brokers, configurations和ACLs。
 * 所需的最小的Kafka broker版本為0.10.0.0。有些API會需要更高版本的Kafka broker的話會註解在API中。
 *
 * == 使用Kafka的AdminClient API來刪除Kafka Topic ==
 */
public class ds01_AdminClient_delete_topics {
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 步驟1. 設定要連線到Kafka集群的相關設定
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Kafka集群在那裡?

        // 步驟2. 創建AdminClient的instance
        AdminClient adminClient = KafkaAdminClient.create(props); // 透過create()來產生adminClient的instance

        // 步驟3. 透過AdminClient的API來操作Kafka Topic (C: Create, U: Update, D: Delete, Q: Query)

        // *** 刪除 "既存的" Kafka Topics  (D: Delete) ***  //

        // *   呼叫 deleteTopics()  *  //

        String topic_to_delete = "topic_01";

        // 取得刪除的結果
        DeleteTopicsResult deleteTopicsResult =  adminClient.deleteTopics(Arrays.asList(topic_to_delete));

        try {
            Map<String, KafkaFuture<Void>> deleteStates = deleteTopicsResult.values();
            for(Map.Entry<String, KafkaFuture<Void>> deleteState : deleteStates.entrySet()) {
                String topic = deleteState.getKey();
                try {
                    deleteState.getValue().get();
                    System.out.println("Topic: [" + topic + "] is deleted");
                } catch (ExecutionException e) {
                    // 檢查
                    if (e.getCause() instanceof UnknownTopicOrPartitionException)
                        // 代表topic不存在
                        System.out.println("Topic: [" + topic + "] is not existed in kafka broker");
                    else
                        e.printStackTrace(); // 有其它不知名的問題
                } catch (Exception e) {
                    e.printStackTrace(); // 有其它不知名的問題
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // 有其它不知名的問題
        }

        // 步驟4. 適當地釋放AdminClient的資源
        adminClient.close();
    }
}