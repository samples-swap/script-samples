class DeliveryNotificationStreamSpec extends Specification {
  /** more configuration hidden from example **/

  int userId = 123
  int otherUserId = 987
  int packageKey = 1234567
  int eventId = 321

  def configureTestDriver() {
    StreamsBuilder builder = new StreamsBuilder()
    def ktable = new UserTable().usersKTable(userSerde, builder)

    new DeliveryNotificationStream().deliveredPackage(
      ktable,
      packageEventSerde,
      userSerde,
      notificationSerde,
      builder)

    testDriver = new TopologyTestDriver(builder.build(), props)
  }

  def setup() {
    configureTestDriver()

    User user = TestData.newUser()
    KeyValueStore store = testDriver.getKeyValueStore("users")
    store.put(userId, user)
  }

  def 'delivered event comes through to the end topic'() {
    given: 'a package event record'
    PackageEvent event = buildEvent(userId, packageKey, EventType.DELIVERED)
    ConsumerRecord record = recordFactory.create(
      Topic.PACKAGE_EVENTS, packageKey, event
    )

    when: 'piping it through the streams topology'
    testDriver.pipeInput(record)

    then: 'a message is published to the delivered events topic'
    ProducerRecord<Integer, Notification> recordFound = testDriver.readOutput(
      Topic.DELIVERY_NOTIFICATIONS,
      new IntegerDeserializer(),
      notificationSerde.deserializer()
    )

    recordFound
    recordFound.value().email == "user@email.com"
  }

  def 'non-delivered event does not come through to the end topic'() {
    given: 'an UNLOADED package event record'
    PackageEvent event = buildEvent(userId, packageKey, EventType.UNLOADED)
    ConsumerRecord record = recordFactory.create(
      Topic.PACKAGE_EVENTS, packageKey, event
    )

    when: 'piping it through the streams topology'
    testDriver.pipeInput(record)

    then: 'a delivery notification is NOT published'
    ProducerRecord<Integer, Notification> recordFound = testDriver.readOutput(
      Topic.DELIVERY_NOTIFICATIONS,
      new IntegerDeserializer(),
      notificationSerde.deserializer()
    )

    !recordFound
  }

  def 'non-matching key does not come through to the end topic'() {
    given: 'a DELIVERED package event record for a non-existent user'
    PackageEvent event = buildEvent(otherUserId, packageKey, EventType.DELIVERED)
    ConsumerRecord record = recordFactory.create(
      Topic.PACKAGE_EVENTS, packageKey, event
    )

    when: 'piping it through the streams topology'
    testDriver.pipeInput(record)

    then: 'a delivery notification is NOT published'
    ProducerRecord<Integer, Notification> recordFound = testDriver.readOutput(
      Topic.DELIVERY_NOTIFICATIONS,
      new IntegerDeserializer(),
      notificationSerde.deserializer()
    )

    !recordFound
  }
}
