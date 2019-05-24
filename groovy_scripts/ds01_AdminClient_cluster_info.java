import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.Node;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Kafka的AdminClient函式庫，支持管理和檢查topics, brokers, configurations和ACLs。
 * 所需的最小的Kafka broker版本為0.10.0.0。有些API會需要更高版本的Kafka broker的話會註解在API中。
 */
public class ds01_AdminClient_cluster_info {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 步驟1. 設定要連線到Kafka集群的相關設定
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Kafka集群在那裡?

        // 步驟2. 創建AdminClient的instance
        AdminClient adminClient = KafkaAdminClient.create(props); // 透過create()來產生adminClient的instance

        // 步驟3. 透過AdminClient的API來取得相關Kafka叢集的訊息

        // *** 取得Kafka叢集的基本資訊 ***  //

        // *   呼叫 describeCluster()  *  //
        DescribeClusterResult result = adminClient.describeCluster();

        // DescribeClusterResult包含了三種資訊

        // Kafka叢集的clusterId
        String clusterId = result.clusterId().get();
        System.out.println("Kafka cluster id: " + clusterId);

        // Kafka叢集的broker nodes
        Collection<Node> nodes = result.nodes().get();

        // Kafka broker node的基本訊息包括了:
        //  id: 不重覆的node id (int)
        //  idString: 以字串表示的node id
        //  host: node的host或ip
        //  port: node的port
        //  rack: node放在那個rack

        for(Node node : nodes) {
            System.out.println("Kafka broker node: " + node);
        }

        // Kafka叢集中目前的controller node (同一個時間只會有一個controller node在叢集中
        Node controllerNode = result.controller().get();

        System.out.println("Kafka controller node: " + controllerNode);

        // 步驟4. 適當地釋放AdminClient的資源
        adminClient.close();
    }
}