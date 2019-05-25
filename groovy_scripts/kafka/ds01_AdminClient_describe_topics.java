import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Kafka的AdminClient函式庫，支持管理和檢查topics, brokers, configurations和ACLs。
 * 所需的最小的Kafka broker版本為0.10.0.0。有些API會需要更高版本的Kafka broker的話會註解在API中。
 */
public class ds01_AdminClient_describe_topics {
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 步驟1. 設定要連線到Kafka集群的相關設定
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Kafka集群在那裡?

        // 步驟2. 創建AdminClient的instance
        AdminClient adminClient = KafkaAdminClient.create(props); // 透過create()來產生adminClient的instance

        // 步驟3. 透過AdminClient的API來操作Kafka Topic (C: Create, U: Update, D: Delete, R: Query)

        // *** 查詢 "既有Kafka Topics"的設定  (R: Query) ***  //

        // *   呼叫 listTopics()      *  //

        // 秀出在Kafka集群中有多少topics的列表
        ListTopicsResult listTopicsResult = adminClient.listTopics();

        // 取得topic的名稱列表
        Set<String> topicsNames = listTopicsResult.names().get();

        topicsNames.forEach(name -> System.out.println("Topic: " + name)); // 使用Java8的forEach方法來打印結果

        // 使用TopicListing物件來識別topic是不是屬于internal topic (比如: __consumer_offsets)
        Collection<TopicListing> topicListings = listTopicsResult.listings().get();

        topicListings.forEach(topicListing -> System.out.println(topicListing)); // 打印

        // 發現不會秀出"__consumer_offset
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true); // 要求列出Kafka的internal topics

        // 再去請求一次 (要求把internal的topic也列出來)
        listTopicsResult = adminClient.listTopics(options);
        listTopicsResult.listings().get().forEach(topicListing -> System.out.println(topicListing)); // 打印

        // *   呼叫 describeTopics()  *  //

        // 把每個Topic的細部訊息都逐個打印出來
        for (String topicName : topicsNames) {
            DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Arrays.asList(topicName));
            printDescribeTopicsResult(describeTopicsResult);
        }

        // 步驟4. 適當地釋放AdminClient的資源
        adminClient.close();
    }

    // 簡單印出Topic的細項訊息
    private static void printDescribeTopicsResult(DescribeTopicsResult ret) throws ExecutionException, InterruptedException {
        Map<String, TopicDescription> topics = ret.all().get();

        for (Map.Entry<String, TopicDescription> entry : topics.entrySet()) {
            // entry的key是TopicName, entry的value是TopicDescription物件
            String topicName = entry.getKey();
            TopicDescription topicDescription = entry.getValue();

            System.out.println("Topic: " + topicName);

            // 每一個Topic都可能有多個Partition
            for (TopicPartitionInfo topicPartitionInfo : topicDescription.partitions()) {
                // 一個PartionInfo包含以下重要資訊:
                //    int partition: Partiton的編號(由0開始)
                //    Node leader: 那一個Broker node是這個partiton的learder
                //    List<Node> replicas: 那些Broker nodes是這個partition的replicas
                //    List<Node> isr: 每一個replicas的ISR (in-sync-replica)

                int partitionId = topicPartitionInfo.partition();
                int leaderNodeId = topicPartitionInfo.leader().id();

                String replicaIds = "";
                for (Node node : topicPartitionInfo.replicas()) {
                    if (replicaIds.isEmpty())
                        replicaIds += node.id() + "";
                    else
                        replicaIds += ", " + replicaIds;
                }

                String isrIds = "";
                for (Node node : topicPartitionInfo.isr()) {
                    if (isrIds.isEmpty())
                        isrIds += node.id() + "";
                    else
                        isrIds += ", " + isrIds;
                }

                System.out.println(String.format("    Topic: %s  Partition: %d    Leader: %d    Replicas: %s    Isr: %s", topicName, partitionId, leaderNodeId, replicaIds, isrIds));
            }
            System.out.println("");
        }
    }
}