class UserTableSpec extends Specification {
	MockSchemaRegistryClient mockClient
	Serde<User> userSerde
	ConsumerRecordFactory recordFactory
	TopologyTestDriver testDriver

	def setup() {
		mockClient = new MockSchemaRegistryClient()
		userSerde = new SpecificAvroSerde<>(mockClient)
		Map serdeProps = [
			(SCHEMA_REGISTRY_URL_CONFIG): 'http://sr:8081',
			(SPECIFIC_AVRO_READER_CONFIG): true
		]
		userSerde.configure(serdeProps, false)

		recordFactory = new ConsumerRecordFactory<Integer, User>(
			new IntegerSerializer(),
			userSerde.serializer()
		)

		// configure StreamsBuilder by reference
		StreamsBuilder builder = new StreamsBuilder()
		new UserTable().usersKTable(userSerde, builder)

		Properties streamProps = [
			(APPLICATION_ID_CONFIG): 'user-ktable-specification',
			(BOOTSTRAP_SERVERS_CONFIG): 'broker:9091'
		]
		testDriver = new TopologyTestDriver(builder.build(), streamProps)
	}

	def 'user message is stored in ktable'() {
		given: 'a user avro message'
		User user = User.newBuilder()
			.setUserId(123)
			.setEmail('john.doe@email.com')
			.setNotificationsEnabled(false)
			.setUserName('john')
			.build()

		ConsumerRecord consumerRecord = recordFactory.create(
				'user-updates', 	// topic
				user.getUserId(),	// key
				user 			// value
		)

		when: 'supplying it to the test driver to process'
		testDriver.pipeInput(consumerRecord)

		then: 'it is stored in the KTable'
		KeyValueStore store = testDriver.getKeyValueStore('users')
		User result = store.get(user.getUserId()) as User

		result
		result == user
	}

  def 'existing user is updated in ktable'() {
      given: 'an existing user in the KTable'
      User user = User.newBuilder()
              .setUserId(123)
              .setEmail('john.doe@email.com')
              .setNotificationsEnabled(false)
              .setUserName('john')
              .build()
      KeyValueStore store = testDriver.getKeyValueStore('users')
      store.put(user.getUserId(), user)

      when: 'enabling notifications on the user'
      user.setNotificationsEnabled(true)
      ConsumerRecord consumerRecord = recordFactory.create(
              'user-updates',
              user.getUserId(),
              user
      )

      and: 'supplying it to the test driver'
      testDriver.pipeInput(consumerRecord)

      then: 'it is updated in the KTable'
      User result = store.get(user.getUserId()) as User

      result
      result.getNotificationsEnabled()
  }
}