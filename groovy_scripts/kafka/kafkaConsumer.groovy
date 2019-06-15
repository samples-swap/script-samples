@Grab(group = "org.apache.kafka", module = "kafka-clients", version = "0.10.2.1")
//@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')
import org.apache.kafka.clients.consumer.KafkaConsumer


Properties props = new Properties()
props.put('zk.connect', '<my-zookeeper>:2181')
props.put('bootstrap.servers', '<kafka-broker 1>:9092,<kafka-broker 2>:9092,<kafka-broker 3>:9092')
props.put('key.deserializer', 'org.apache.kafka.common.serialization.StringDeserializer')
props.put('value.deserializer', 'org.apache.kafka.common.serialization.StringDeserializer')
props.put('group.id', 'groovy-consumer')

def topics = ['<my topic>']

def consumer = new KafkaConsumer(props)
consumer.subscribe(topics)

while (true) {
    def consumerRecords = consumer.poll(1000)

    consumerRecords.each{ record ->
        println record.key()
        println record.value()
    }

    consumer.commitAsync();
}
consumer.close();
