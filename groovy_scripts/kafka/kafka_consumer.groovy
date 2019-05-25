String consume(String topic, String key, int maxRetries, long pollMs) {
    int retry = 0
    String message = null
    KafkaConsumer<String, String> consumer = createKafkaConsumer(topic)

    while (!message && retry < maxRetries) {
        retry++
        ConsumerRecords consumerRecords = consumer.poll(pollMs)
        consumerRecords.each { ConsumerRecord record ->
            if (record.key() == key) {
                message = record.value()
            }
        }  
        consumer.commitSync()
    }
    consumer.close()
    return message
 }