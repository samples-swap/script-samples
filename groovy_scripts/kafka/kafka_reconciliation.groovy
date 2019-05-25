/*
Kakfa reconciliation:
These scripts have been written to be able to produce and consume messages from a topic whilst reconfiguring Kafka brokers and migrating Kafka to a new Zookeeper ensemble. I am paranoid, so I wanted to be sure that no messages were dropped whilst making changes to a running Kafka cluster. These scripts help to verify that there are no lost messages.

Assumptions fo running the scripts:

There is a running Kafka cluster with a topic a-topic configured with 3 replicas (did I mention that I don't want to lose messages?).
JDK 1.8 and Groovy installed.
Usage Instructions
Start the consumer.groovy script on a machine that can communicate with your Kafka cluster. I run this backgrounded in nohup mode so I can exit the machine without killing the process: nohup groovy consumer &.
Start the producer.groovy script as above: nohup groovy producer &.
Make changes to your cluster, e.g. swap out Zookeeper nodes, rotate config on your Kafka nodes, add new nodes etc.
Kill the process running the producer.groovy script.
Tail the nohup.out file (from the directory running the consumer.groovy script: tail -f nohup.out) and wait until no more messages are being consumed.
Kill the process running the consumer.groovy script.
Run the reconciliation.groovy script to determine if any messages were dropped.
Reconciliation Output
If you see output lines specifying that the 'Consumer did not receive message: ...' then messages were successfully sent to your topic but never consumed by the consumer. Things to check:

Is your topic set up with replicas? e.g. --replication-factor 3 when creating the topic.
Is your producer configured for ack=all?
Kafka broker configuration for unclean leader election unclean.leader.election.enable=false (this is the default behaviour).
If you see output lines specifying that the 'Consumer read message ... before Producer acknowledgement.' then a message was dropped by the Producer in flight, but the consumer was able to read it from a node before it was replicated across the cluster. This is likely to happen when restarting/removing/rotating a Kafka node if not using the 'Exactly once semantics'. Kafka topics by default provide at-least-once delivery of messages. In this scenario, a consumer would have read the message from the master node of a particular record before that record had been replicated to the other nodes of the cluster; when t
*/
#!/usr/bin/env groovy

@Grapes([
    @Grab('com.h2database:h2:1.3.176'),
    @Grab('org.apache.kafka:kafka_2.11:0.10.2.0'),
    @GrabConfig(systemClassLoader=true)
])

import java.sql.*
import groovy.sql.Sql
import org.h2.jdbcx.JdbcConnectionPool
import org.apache.kafka.clients.consumer.*

println "Downloaded dependencies."

def sql = Sql.newInstance("jdbc:h2:reconciliation;AUTO_SERVER=TRUE", "sa", "sa", "org.h2.Driver")
createRecordsTable(sql, "consumer_record")

def consumer = new KafkaConsumer<>(consumerProperties)
def topics = ["a-topic"]
consumer.subscribe(topics)
println "Subscribed to topics $topics."

boolean hasRecords = false
while (true) {
    consumer.poll(1000).each { record ->
        // Kafka provides at-least-once delivery of events; therefore, we may consume duplicates.
        // Only add the record if we have not seen the id before.
        sql.execute("""
            INSERT INTO consumer_record(id) SELECT ${record.key()} WHERE NOT EXISTS(
                SELECT * FROM consumer_record WHERE id = ${record.key()}
            )
        """)
        println "Read message ${record.value()} at offset ${record.offset()}."
        hasRecords = true
    }
    if(hasRecords) {
        consumer.commitSync()
        println "Committed offset."
        hasRecords = false
    }
    println 'Awaiting records.'
}

def createRecordsTable(def sql, String tableName) {
    sql.execute("DROP TABLE IF EXISTS $tableName" as String)
    sql.execute("CREATE TABLE $tableName(id VARCHAR(60) PRIMARY KEY)" as String)
    println "Created $tableName table."
}

def getConsumerProperties() {
    Properties properties = new Properties()
    properties.put("bootstrap.servers", "localhost:9092") // 10.4.1.10
    properties.put("group.id", "test")
    properties.put("enable.auto.commit", "false")
    properties.put("auto.offset.reset", "earliest")
    properties.put("session.timeout.ms", "30000")
    properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties
}


#!/usr/bin/env groovy

@Grapes([
    @Grab('com.h2database:h2:1.3.176'),
    @Grab('org.apache.kafka:kafka_2.11:0.10.2.0'),
    @GrabConfig(systemClassLoader=true)
])

import java.sql.*
import groovy.sql.Sql
import org.h2.jdbcx.JdbcConnectionPool
import org.apache.kafka.clients.producer.*

println "Downloaded dependencies."

// Create a local H2 database for reconciling the produced and consumed messages
// AUTO_SERVER=TRUE to allow for the producer and the consumer to write to the db at the same time.
def sql = Sql.newInstance("jdbc:h2:reconciliation;AUTO_SERVER=TRUE", "sa", "sa", "org.h2.Driver")
createRecordsTable(sql, "producer_record")

Producer<String, String> producer = new KafkaProducer<>(producerProperties)
println "Created KafkaProducer."

def records = sql.dataSet("producer_record")

while(true) {
    def uuid = UUID.randomUUID().toString()
    println "Sending message $uuid."
    producer.send(
            new ProducerRecord<String, String>("a-topic", uuid,   uuid),
            { Object[] args ->
                if(args[1]) {
                    println "Error: ${args[1]}."
                } else {
                    records.add(id: uuid)
                    println "Sent message $uuid at offset: ${args[0].offset()}."
                }
            } as Callback
    )
    Thread.sleep(1000)
}

def createRecordsTable(def sql, String tableName) {
    sql.execute("DROP TABLE IF EXISTS $tableName" as String)
    sql.execute("CREATE TABLE $tableName(id VARCHAR(60) PRIMARY KEY)" as String)
    println "Created $tableName table."
}

def getProducerProperties() {
    Properties properties = new Properties()
    properties.put("bootstrap.servers", "localhost:9092")
    properties.put("acks", "all")
    properties.put("retries", 0)
    properties.put("max.block.ms", 800)
    properties.put("batch.size", 16384)
    properties.put("linger.ms", 1)
    properties.put("buffer.memory", 33554432)
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties
}

	@Grapes([
    @Grab('com.h2database:h2:1.3.176'),
    @GrabConfig(systemClassLoader=true)
])
 
import java.sql.*
import groovy.sql.Sql
import org.h2.jdbcx.JdbcConnectionPool

def sql = Sql.newInstance("jdbc:h2:reconciliation;AUTO_SERVER=TRUE", "sa", "sa", "org.h2.Driver")

sql.eachRow("SELECT p.id FROM producer_record p WHERE NOT EXISTS(SELECT * FROM consumer_record WHERE id = p.id)") {
    println "Consumer did not receive message: ${it.id}."
}

sql.eachRow("SELECT c.id FROM consumer_record c WHERE NOT EXISTS(SELECT * FROM producer_record WHERE id = c.id)") {
    println "Consumer read message ${it.id} before Producer acknowlegement."
}

sql.eachRow("SELECT COUNT(1) AS total FROM producer_record") {
    println "Total unique produced messages: ${it.getInt('total')}."
}
sql.eachRow("SELECT COUNT(1) AS total FROM consumer_record") {
    println "Total unique consumed messages: ${it.getInt('total')}."
}