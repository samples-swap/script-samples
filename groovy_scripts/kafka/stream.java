public KStream<Integer, Notification> deliveredPackage(
	KTable<Integer, User> usersKTable,
	Serde<PackageEvent> packageEventSerde,
	Serde<User> userSerde,
	Serde<Notification> notificationSerde,
	StreamsBuilder streamsBuilder
) {
	KStream<Integer, Notification> stream = streamsBuilder
		.stream(
			Topic.PACKAGE_EVENTS,
			Consumed.with(Serdes.Integer(), packageEventSerde)
		)
		.filter((key, event) -> event.getEventType().equals("DELIVERED"))
		.selectKey((key, value) -> value.getUserId())
		.join(
			usersKTable,
			(event, user) -> Notification
				.newBuilder()
				.setEmail(user.getEmail())
				.setUserId(user.getUserId())
				.setType(event.getEventType())
				.build(),
			Joined.with(Serdes.Integer(), packageEventSerde, userSerde)
		);
		
	stream.to(
		Topic.DELIVERY_NOTIFICATION,
		Produced.with(
			Serdes.Integer(), 
			notificationSerde
		)
	);

	return stream;
}