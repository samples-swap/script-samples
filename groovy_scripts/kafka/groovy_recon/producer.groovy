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