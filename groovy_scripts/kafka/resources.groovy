    Map producerTestConfig = [:]

    if (Environment.current == Environment.TEST) {
        Path producerLogPath = Files.createTempFile('producer', '.log')
        loggingProducerOutput(LoggingProducerOutput) {
            logPath = producerLogPath
        }
        producerTestConfig['interceptor.classes'] = LoggingProducerInterceptor.name
        producerTestConfig["interceptor.${LoggingProducerInterceptor.simpleName}.file".toString()] = producerLogPath.toString()
    }

    domainEventProducer(KafkaProducer, commonKafkaConfig + producerTestConfig + [
            'key.serializer'      : 'org.apache.kafka.common.serialization.StringSerializer',
            'value.serializer'    : 'org.apache.kafka.common.serialization.StringSerializer',
            'acks'                : '1',
            'linger.ms'           : '100',
    ])