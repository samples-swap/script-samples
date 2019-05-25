@Grab('org.apache.kafka:kafka-clients:2.0.0')
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

import groovy.json.JsonOutput

class JsonSerializer implements Serializer {
    @Override
    void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    byte[] serialize(String topic, data) {
        JsonOutput.toJson(data).getBytes('UTF-8')
    }

    @Override
    void close() {
    }
}

def props = [
    'bootstrap.servers': 'localhost:9092',
    'key.serializer': StringSerializer,
    'value.serializer': JsonSerializer
]

def topic = 'groovy-events'

def producer = new KafkaProducer(props)

['Groovy', 'Grails', 'Micronaut'].each {
    def record = new ProducerRecord(topic, [msg: "$it rocks!"])
    def metadata = producer.send(record).get()

    println "Record sent to partition ${metadata.partition()}, with offset ${metadata.offset()}"
}

@Grab('org.apache.kafka:kafka-clients:2.0.0')
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer

import groovy.json.JsonSlurper

class JsonDeserializer implements Deserializer {
    def slurper = new JsonSlurper()

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    def deserialize(String topic, byte[] data) {
        slurper.parse(data)
    }

    @Override
    void close() {
    }
}

def props = [
    'bootstrap.servers': 'localhost:9092',
    'key.deserializer': StringDeserializer,
    'value.deserializer': JsonDeserializer,
    'group.id': 'GroovyScriptConsumer'
]

def topics = ['groovy-events']

// This closure handles a Kafka message
def consumeRecord = { record ->
    println "Consumer Record: (${record.key()}, ${record.value()}, ${record.partition()}, ${record.offset()})"
}

def consumerThread = new Thread() {
    def terminated = false

    void run() {
        def consumer = new KafkaConsumer(props)
        consumer.subscribe(topics)

        while (!terminated) {
            def records = consumer.poll(1_000)
            records.each consumeRecord
            consumer.commitAsync()
        }

        consumer.close()
        println 'Consumer ended'
    }
}

consumerThread.start()

addShutdownHook {
    consumerThread.with {
        terminated = true
        join()
    }  
}