import groovy.util.logging.Slf4j
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Logs messages sent to a Kafka producer to a YAML file for use by test verifications. This class is
 * not intended for use in production.
 * Use configuration "interceptor.LoggingProducerInterceptor.file" to specify the output file, otherwise a
 * temporary unique file name will be chosen.
 */
@Slf4j
class LoggingProducerInterceptor implements ProducerInterceptor {
 static ThreadLocal<Yaml> YAML = new ThreadLocal<Yaml>() {
 @Override
 protected Yaml initialValue() {
 new Yaml(new DumperOptions(defaultFlowStyle: DumperOptions.FlowStyle.FLOW))
 }
 }

 Path logPath

 @Override
 void configure(Map<String, ?> configs) {
 String configuredLogPath = configs.get("interceptor.${LoggingProducerInterceptor.simpleName}.file".toString())
 logPath = configuredLogPath ? Paths.get(configuredLogPath) : Files.createTempFile('producer', '.log')
 try {
 logPath.withWriterAppend {
 it.write('- ')
 YAML.get().dump([configs: configs], it)
 }
 } catch (IOException e) {
 // ignore, in case of multiple threads
 log.debug("Writing configs to ${logPath}", e)
 }
 }

 @Override
 ProducerRecord onSend(ProducerRecord record) {
 int retries = 3
 while (retries-- > 0) {
 try {
 logPath.withWriterAppend {
 it.write('- ')
 YAML.get().dump([thread: Thread.currentThread().id, key: record.key(), value: record.value()], it)
 }
 retries = 0
 } catch (IOException e) {
 // ignore and retry, in case of multiple threads
 log.debug("Writing producer record to ${logPath}", e)
 Thread.sleep(100)
 }
 }
 return record
 }

 @Override
 void onAcknowledgement(RecordMetadata metadata, Exception exception) {
 // nothing to do
 }

 @Override
 void close() {
 Files.delete(logPath)
 }
}