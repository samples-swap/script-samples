import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Kafka的AdminClient函式庫，支持管理和檢查topics, brokers, configurations和ACLs。
 * 所需的最小的Kafka broker版本為0.10.0.0。有些API會需要更高版本的Kafka broker的話會註解在API中。
 */
public class ds01_AdminClient_create_topics {
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 步驟1. 設定要連線到Kafka集群的相關設定
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Kafka集群在那裡?

        // 步驟2. 創建AdminClient的instance
        AdminClient adminClient = KafkaAdminClient.create(props); // 透過create()來產生adminClient的instance

        // 步驟3. 透過AdminClient的API來操作Kafka Topic (C: Create, U: Update, D: Delete, Q: Query)

        // *** 產生"新的"Kafka Topics  (C: Create) ***  //

        // *   呼叫 createTopics()  *  //

        // 要創建一個新的Topic要使用"NewTopic"物件來定義相關的設定, 包括了最基本的:
        // 1. name
        // 2. partition count
        // 3. replication factor
        // 4. others ...

        // 第1個topic
        String topic_name1 = "topic_01";
        int partition_count1 = 1;
        short replication_factor1 = 1;

        // 第1個topic的新建的請求物件
        NewTopic topic_01 = new NewTopic(topic_name1, partition_count1, replication_factor1);

        // 第2個topic
        String topic_name2 = "topic_02";
        int partition_count2 = 3;
        short replication_factor2 = 1;

        // 第2個topic的新建的請求物件
        NewTopic topic_02 = new NewTopic(topic_name2, partition_count2, replication_factor2);

        // 第3個topic (設定"cleanup.policy=compact")
        String topic_name3 = "topic_03";
        int partition_count3 = 3;
        short replication_factor3 = 1;

        Map<String, String> configs = new HashMap<>();
        configs.put("cleanup.policy","compact");

        // 第3個topic的新建的請求物件
        NewTopic topic_03 = new NewTopic(topic_name3, partition_count3, replication_factor3);
        topic_03.configs(configs); // 把其它設定放在一個Map<String, String>物件中

        // 呼叫AdminClient/db. API來創建topics
        CreateTopicsResult result = adminClient.createTopics(Arrays.asList(topic_01, topic_02, topic_03));

        // 檢查結果 entry的key是topic_name, value是KafkaFuture
        for(Map.Entry entry : result.values().entrySet()) {
            String topic_name = (String) entry.getKey();

            // 由於產生topics需要花點時間, 透過sync的method來了解topic的創建的進度。
            // 但是API的回傳是"Void", 所以並不知道結果是成功還是失敗, 必需透過Try..Catch來偵測
            boolean success = true;
            String error_msg = "";

            try {
                ((KafkaFuture<Void>) entry.getValue()).get();
            } catch (Exception e) {
                success = false;
                error_msg = e.getMessage();
            }

            if (success)
                System.out.println("Topic: " + topic_name + " creation process completed!");
            else
                System.out.println("Topic: " + topic_name + " creat fail, due to [" + error_msg + "]");
        }

        // 步驟4. 適當地釋放AdminClient的資源
        adminClient.close();
    }
}