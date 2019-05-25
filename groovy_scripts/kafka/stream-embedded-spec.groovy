@SpringBootTest
class DeliveryNotificationEmbeddedSpec extends Specification {
  @Autowired
  StreamsBuilderFactoryBean factoryBean
  
  /** wire in consumers/producers for test **/

  public static KafkaEmbedded kafkaEmbedded = new KafkaEmbedded(
    brokerCount,
    controlledShutdown,
    'user-updates',
    'package-events',
    'delivery-notifications'
  )

  def setupSpec() {
    kafkaEmbedded.before()
  }

  def cleanupSpec() {
    kafkaEmbedded.after()
  }

  def 'delivery notification is published to topic'() {
    given: 'a user update published and stored in the ktable'
    User user = TestData.buildUser(userId)
    publishUserUpdate(user)

    and: 'a DELIVERED package event ready to be published'
    PackageEvent event = PackageEvent
      .newBuilder()
      .setEventId(1)
      .setUserId(user.userId)
      .setPackageId(345)
      .setEventType(EventType.DELIVERED)
      .build()

    when: 'publishing the event'
    publishPackageEvent(event)

    then: 'notification is sent to topic'
    List<Notification> notifications = readValues(
      Topic.DELIVERY_NOTIFICATIONS,
      notificationConsumer,
      maxMessages
    )

    and: 'has the expected user values and type'
    notifications.first() == Notification
      .newBuilder()
      .setUserId(user.userId)
      .setEmail(user.email)
      .setType('DELIVERY')
      .build()
  }

  def 'delivery notification is NOT published to topic'() {
    given: 'a user update published and stored in the ktable'
    User user = TestData.buildUser(userId)
    publishUserUpdate(user)

    and: 'a UNLOADED package event ready to be published'
    PackageEvent event = PackageEvent
      .newBuilder()
      .setEventId(1)
      .setUserId(user.userId)
      .setPackageId(345)
      .setEventType(EventType.UNLOADED)
      .build()

    when: 'publishing the event'
    publishPackageEvent(event)

    then: 'notification is NOT sent to topic'
    List<Notification> notifications = readValues(
      Topic.DELIVERY_NOTIFICATIONS,
      notificationConsumer,
      maxMessages
    )

    notifications.isEmpty()
  }
}


static <K, V> List<KeyValue<K, V>> readKeyValues(
  String topic, Consumer consumer, int maxMessages) {
  consumer.subscribe([topic])

  int pollIntervalMs = 100
  int maxTotalPollTimeMs = 2000 // 2s
  int totalPollTimeMs = 0
  
  List<KeyValue<K, V>> consumedValues = []
  while (totalPollTimeMs < maxTotalPollTimeMs && consumedValues.size() < maxMessages) {
    totalPollTimeMs += pollIntervalMs
    ConsumerRecords<K, V> records = consumer.poll(pollIntervalMs)
    for (ConsumerRecord<K, V> record : records) {
      consumedValues << new KeyValue<>(record.key(), record.value())
    }
  }

  consumer.close()
  return consumedValues
}