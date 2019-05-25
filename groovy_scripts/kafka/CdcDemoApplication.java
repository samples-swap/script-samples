// For further information follow the instructions at: 
// https://github.com/tzolov/cdc-debezium/tree/master/spring-cloud-starter-stream-common-cdc/cdc-spring-boot-starter
@SpringBootApplication
@AutoConfigureBefore(CdcAutoConfiguration.class)
public class CdcDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CdcDemoApplication.class, args);
	}

	@Bean
	public Consumer<SourceRecord> mySourceRecordConsumer() {
		return sourceRecord -> {
			System.out.println(" My handler: " + sourceRecord.toString());
		};
	}

}