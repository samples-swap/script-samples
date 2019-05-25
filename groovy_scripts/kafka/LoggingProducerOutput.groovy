port groovy.transform.AutoClone
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path

/**
 * Provides the output of LoggingProducerInterceptor as an Iterable so Groovy constructs can be used to filter and
 * process the YAML output.
 */
@AutoClone
class LoggingProducerOutput implements Iterable {
 static ThreadLocal<Yaml> YAML = new ThreadLocal<Yaml>() {
 @Override
 protected Yaml initialValue() {
 new Yaml(new DumperOptions(defaultFlowStyle: DumperOptions.FlowStyle.FLOW))
 }
 }

 /** The path of the YAML log file. */
 Path logPath
 private boolean limitToCurrentThread
 private Integer startingEvent

 private Iterator all() {
 logPath.withReader {
 YAML.get().load(it).iterator()
 }
 }

 @Override
 Iterator iterator() {
 Iterator result = all()
 if (startingEvent != null) {
 result = result.drop(startingEvent)
 }
 if (limitToCurrentThread) {
 result = result.findAll { it.thread == Thread.currentThread().id }.iterator()
 }
 result
 }

 /**
 * Limits the results to only the current Thread id. Note that per JDK the Thread id may be reused.
 */
 Iterator findAllByThread() {
 iterator().findAll { it.thread == Thread.currentThread().id }.iterator()
 }

 /**
 * Return a new object limited to the current thread. Note that per JDK the Thread id may be reused.
 */
 LoggingProducerOutput withThread() {
 LoggingProducerOutput result = clone()
 result.limitToCurrentThread = true
 result
 }

 /**
 * Return a new object excluding all current messages.
 */
 LoggingProducerOutput withEmpty() {
 LoggingProducerOutput result = clone()
 result.startingEvent = all().size()
 result
 }
}