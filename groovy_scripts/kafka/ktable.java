@Configuration
public class UserTable {
	@Bean
	public KTable<Integer, User> usersKTable(
		Serde<User> userSerde, 
		StreamsBuilder streamsBuilder) {

		return streamsBuilder.table(
			Topic.USER_UPDATES,
			Materialized.<Integer, User, KeyValueStore<Bytes, byte[]>>
				as("users") // ktable name
					.withKeySerde(Serdes.Integer())
					.withValueSerde(userSerde)
		);
	}
}