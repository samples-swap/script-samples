@Bean
SchemaRegistryClient mockClient() {
  return new MockSchemaRegistryClient()
}

@Bean
Serde<PackageEvent> packageEventSerde(KafkaProperties props) {
  Serde<PackageEvent> serde = SpecificAvroSerde<>(mockClient())
  serde.configure(
    props.buildConsumerProperties(), 
    false // isKey
  )

  return serde
}

@Bean
KafkaAvroSerializer kafkaAvroSerializer(KafkaProperties props) {
  return new KafkaAvroSerializer(
    mockClient(), 
    props.buildProducerProperties()
  )
}

@Bean
KafkaAvroDeserializer kafkaAvroDeserializer(KafkaProperties props) {
  return new KafkaAvroDeserializer(
    mockClient(), 
    props.buildConsumerProperties()
  )
}