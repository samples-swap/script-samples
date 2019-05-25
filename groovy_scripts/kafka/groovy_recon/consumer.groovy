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