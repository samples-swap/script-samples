@Bean
KTable reportStream(StreamsBuilder builder, Engine engine) {

        def stream = builder.stream(topic)
                .groupBy({ key, word -> word })
                .windowedBy(SessionWindows.with(TimeUnit.SECONDS.toMillis(1)))
                .aggregate(
                new Initializer<Long>() {
                    @Override
                    Long apply() {
                        0
                    }
                },
                new Aggregator<String, String, Long>() {
                    @Override
                    Long apply(String key, String value, Long aggregate) {
                        def l = 1 + aggregate
                        return l
                    }
                },
                new Merger() {
                    @Override
                    Long apply(Object aggKey, Object aggOne, Object aggTwo) {
                        return aggOne + aggTwo
                    }
                },
                Materialized.with(Serdes.String(), Serdes.Long()))

        stream.toStream().to("classificationResult")
        
        stream
    }

@Bean
KStream classificationStream(StreamsBuilder builder, Engine engine) {

        builder.stream("classificationResult").mapValues({
            println "classResult"
            println it
        })

}