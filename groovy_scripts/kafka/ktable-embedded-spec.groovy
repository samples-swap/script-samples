@SpringBootTest
@EmbeddedKafka(topics = [
  'user-updates', 
  'package-events', 
  'delivery-notifications'
])
class UserTableEmbeddedSpec extends Specification {
  @Autowired
  StreamsBuilderFactoryBean streamsFactory

  @Autowired
  Producer<Integer, User> userProducer

  def 'user message is stored in ktable'() {
    given: 'a user avro object'
    Integer userId = 123
    User user = TestData.buildUser(userId)
    
    when: 'publishing them to the user-updates topic'
    producer.send(new ProducerRecord<>('user-updates', userId, user))
    
    then: 'the user should be stored in the ktable'
    KafkaStreams streams = streamsFactory.kafkaStreams
    ReadOnlyKeyValueStore<Integer, User> store = streams.store(
      'users', 	// store name
      QueryableStoreTypes.<Integer, User> keyValueStore() // store type
    )
    
    store.get(userId) == user
  }
}