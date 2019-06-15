@Grab(group = "org.apache.kafka", module = "kafka-clients", version = "0.10.2.1")
//@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata


Properties props = new Properties()
props.put('zk.connect', '<my-zookeeper>:2181')
props.put('bootstrap.servers', '<kafka-broker 1>:9092,<kafka-broker 2>:9092,<kafka-broker 3>:9092')
props.put('key.serializer', 'org.apache.kafka.common.serialization.StringSerializer')
props.put('value.serializer', 'org.apache.kafka.common.serialization.StringSerializer')

def producer = new KafkaProducer(props)

def messageSender = { String topic, String message ->
    String key = new Random().nextLong()
    String compoundMessage = "key: $key, message: $message"
    producer.send(
            new ProducerRecord<String, String>(topic, key, compoundMessage),
            { RecordMetadata metadata, Exception e ->
                println "The offset of the record we just sent is: ${metadata.offset()}"
            } as Callback
    )
}
args.each { arg ->
    println "sending message $arg"
    messageSender('<my topic>', arg)
}
producer.close()