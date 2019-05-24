
String consume(String topic, String key, int maxRetries, long pollMs) {
 int retry = 0
 String message = null
 KafkaConsumer<String, String> consumer = createKafkaConsumer(topic)

 while (!message && retry < maxRetries) {
 retry++
 ConsumerRecords consumerRecords = consumer.poll(pollMs)
 consumerRecords.each { ConsumerRecord record ->
 if (record.key() == key) {
 message = record.value()
 }
 }
 consumer.commitSync()
 }
 consumer.close()
 return message
 }
class MyRequestService {
 
 private HttpClient httpClient

 @Inject
 MyRequestService(HttpClient httpClient) {
 this.httpClient = httpClient
 }

 Promise<ReceivedResponse> makeRequest(String uri, String body) {
 httpClient.request(uri.toURI()) {
 it.method('POST')
 .body {
 it.text(body)
 }
 .headers {
 it.set('X-My-Header', 'xyz')
 }
 }
 }
}

OAuthProfile getProfile(OAuthService authService, Token accessToken) {

 OAuthRequest request = new OAuthRequest(Verb.GET, 'https://www.googleapis.com/oauth2/v1/userinfo')
 authService.signRequest(accessToken, request)

 def response = request.send()

 def user = JSON.parse(response.body)
 def login = "${user.given_name}.${user.family_name}".toLowerCase()
 new OAuthProfile(username: login, email: user.email, uid: user.id, picture: user.picture)
 }

@groovy.lang.Grab("com.netflix.rxnetty:rx-netty:0.3.14")

import io.netty.buffer.ByteBuf
import io.reactivex.netty.RxNetty
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerPipelineConfigurator
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import io.reactivex.netty.protocol.http.server.RequestHandler
import rx.functions.Func1

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

def port = 8192

HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(port, new RequestHandler<ByteBuf, ByteBuf>() {
 @Override
 public rx.Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
 final long start = System.nanoTime()
 return request.getContent().map(new Func1<ByteBuf, Void>() {
 @Override
 public Void call(ByteBuf byteBuf) {
 long elapsed = System.nanoTime() - start
 println "Took $elapsed nanos, " + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS) + " ms"
	def body = byteBuf.toString(Charset.defaultCharset());
	println "Got : " + body
 response.writeString(body.toUpperCase());
 return null;
 }
 }).ignoreElements();
 }
}).pipelineConfigurator(new HttpServerPipelineConfigurator<ByteBuf, ByteBuf>()).build();

System.out.println("Starting server...");

server.startAndWait()

class ExampleSpec extends spock.lang.Specification {
 LoggingProducerOutput output
 
 void setup() {
 output = new LoggingProducerOutput(logPath: Paths.get('/tmp/producer.log')).withEmpty()
 }
 
 void "records should be produced"() {
 when: "a record is produced"
 String key = "the_record_key"
 // ...
 then: "record is found in log"
 output.find { it.key == key }
 }
}

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

import groovy.transform.AutoClone
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

{configs: {compression.type: snappy, value.serializer: org.apache.kafka.common.serialization.StringSerializer, interceptor.LoggingProducerInterceptor.file: /var/folders/3v/05f6zqc164g*b55zrdlhk*z00000gn/T/producer3683756155677198489.log, acks: '1', bootstrap.servers: 'localhost:32818', interceptor.classes: LoggingProducerInterceptor, key.serializer: org.apache.kafka.common.serialization.StringSerializer, client.id: api, linger.ms: '100'}}- {thread: 10, key: 5af19764bfe52300144eea35, value: '{"_links":{"self":{"href":"http://169.254.169.254:8080/app/api/user/doe","hreflang":"en","type":"user"}},"id":"5af19764bfe52300144eea35","name":"John Doe"}'}- {thread: 10, key: 5af19764bfe52300144eea35, value: '{"_links":{"self":{"href":"http://169.254.169.254:8080/app/api/user/doe","hreflang":"en","type":"user"}},"id":"5af19764bfe52300144eea35","seasons":"John Doe"}'}

 Map producerTestConfig = [:]

 if (Environment.current == Environment.TEST) {
 Path producerLogPath = Files.createTempFile('producer', '.log')
 loggingProducerOutput(LoggingProducerOutput) {
 logPath = producerLogPath
 }
 producerTestConfig['interceptor.classes'] = LoggingProducerInterceptor.name
 producerTestConfig["interceptor.${LoggingProducerInterceptor.simpleName}.file".toString()] = producerLogPath.toString()
 }

 domainEventProducer(KafkaProducer, commonKafkaConfig + producerTestConfig + [
 'key.serializer' : 'org.apache.kafka.common.serialization.StringSerializer',
 'value.serializer' : 'org.apache.kafka.common.serialization.StringSerializer',
 'acks' : '1',
 'linger.ms' : '100',
 ])

class MockExecAction extends DefaultExecAction {
 int exitValue = 0
 String output = ''

 MockExecAction() {
 super(null, null, null)
 }

 @Override
 ExecResult execute() {
 standardOutput.write(output.getBytes('UTF-8'))

 ExecResult result = new ExecResult() {
 @Override
 int getExitValue() {
 MockExecAction.this.exitValue
 }

 @Override
 ExecResult assertNormalExitValue() throws ExecException {
 if (exitValue != 0) {
 throw new ExecException("Command exited with ${exitValue}: ${getCommandLine().join(' ')}")
 }
 this
 }

 @Override
 ExecResult rethrowFailure() throws ExecException {
 assertNormalExitValue()
 }
 }
 result.assertNormalExitValue()
 result
 }
}

class MockExecAction extends DefaultExecAction {
 int exitValue = 0
 String output = ''

 MockExecAction() {
 super(null, null, null)
 }

 @Override
 ExecResult execute() {
 standardOutput.write(output.getBytes('UTF-8'))

 ExecResult result = new ExecResult() {
 @Override
 int getExitValue() {
 MockExecAction.this.exitValue
 }

 @Override
 ExecResult assertNormalExitValue() throws ExecException {
 if (exitValue != 0) {
 throw new ExecException("Command exited with ${exitValue}: ${getCommandLine().join(' ')}")
 }
 this
 }

 @Override
 ExecResult rethrowFailure() throws ExecException {
 assertNormalExitValue()
 }
 }
 result.assertNormalExitValue()
 result
 }
}

dependencies {
 compile "com.bmuschko:gradle-docker-plugin:3.6.0"
 compile 'org.yaml:snakeyaml:1.17'
}

import com.bmuschko.gradle.docker.DockerRegistryCredentials
import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask
import com.bmuschko.gradle.docker.tasks.RegistryCredentialsAware
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

/**
 * Copies Docker images from one registry to another. The input is a JSON file intended for AWS CodeDeploy specifying
 * which ECS containers to update.
 */
@Slf4j
class ImageCopyTask extends AbstractDockerRemoteApiTask implements RegistryCredentialsAware {
 @InputFile
 File imageDefinitions

 @OutputFile
 @Optional
 File copiedDefinitions

 /**
 * The target Docker registry credentials.
 */
 @Nested
 @Optional
 DockerRegistryCredentials registryCredentials

 ImageCopyTask() {
 onlyIf { registryCredentials != null }
 }

 void runRemoteCommand(dockerClient) {
 def authConfig = threadContextClassLoader.createAuthConfig(getRegistryCredentials())
 URL repoUrl
 if (registryCredentials.url.contains('://')) {
 repoUrl = new URL(registryCredentials.url)
 } else {
 repoUrl = new URL('https://'+registryCredentials.url)
 }
 def images = new JsonSlurper().parse(imageDefinitions)
 def copiedImages = []
 images.each {
 String definitionName = it['name']
 String originalImage = it['imageUri']
 def match = originalImage =~ /(?:.*\/)(.+)(?::(.*))/
 if (match) {
 String name = match[0][1]
 String tag = match[0][2] ?: 'latest'
 String newImage = (repoUrl.port > 0) ? "${repoUrl.host}:${repoUrl.port}/${name}":"${repoUrl.host}/${name}"
 log.info "Copying ${originalImage} to ${newImage}:${tag}"

 log.info "Pulling ${originalImage}"
 def pullImageCmd = dockerClient.pullImageCmd(originalImage-":${tag}").withTag(tag)
 pullImageCmd.exec(threadContextClassLoader.createPullImageResultCallback(onNext)).awaitSuccess()
 def inspectImage = dockerClient.inspectImageCmd(originalImage).exec()

 log.info "Tagging ${originalImage} as ${newImage}:${tag}"
 def tagImageCmd = dockerClient.tagImageCmd(inspectImage.id, newImage, tag)
 tagImageCmd.exec()

 log.info "Pushing ${newImage}:${tag}"
 def pushImageCmd = dockerClient.pushImageCmd(newImage)
 .withTag(tag)
 .withAuthConfig(authConfig)
 pushImageCmd.exec(threadContextClassLoader.createPushImageResultCallback(onNext)).awaitSuccess()

 copiedImages << [name: definitionName, imageUri: "${newImage}:${tag}"]
 }
 }
 if (copiedDefinitions != null) {
 copiedDefinitions.text = JsonOutput.prettyPrint(JsonOutput.toJson(copiedImages))
 }
 }
}

version: "3"

volumes:
 consul:

services:
 mongodb:
 image: pdouble16/autopilotpattern-mongodb:3.6.3-r3
 api:
 image: ${PRIVATE_DOCKER_REGISTRY}/api:${APPLICATION_VERSION:-latest}
 consul:
 image: pdouble16/autopilotpattern-consul:1.0.6-r1

'
version: "3"

volumes:
 consul:

services:
 consul:
 image: consul:latest


import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml

import java.util.regex.Pattern

/**
 * Creates a JSON file suitable for deployment using AWS CodePipeline + ECS deploy. It describes the docker
 * images pushed and the containers that should be updated. The images are taken from a set of docker files.
 */
@CacheableTask
class ImageDefinitionsTask extends DefaultTask {
 protected static final Pattern VARIABLE = ~/\$\{(.*?)}/

 /** The destination for the JSON file. */
 @OutputFile
 File output

 /** The name to resolve variables in docker files. */
 @Input
 Map<String, CharSequence> environment = [:]

 /** List of services to only include in the result. */
 @Input
 @Optional
 List<String> only = []

 @InputFiles
 List<File> dockerComposeFiles = []

 @TaskAction
 void createOutput() {
 Map<String, CharSequence> images = [:]

 Yaml parser = new Yaml()
 dockerComposeFiles.each { File file ->
 Map<String, Object> yaml = Collections.emptyMap()
 file.withInputStream { yaml = parser.loadAs(it, Map) }
 yaml['services']?.each { k,v ->
 if (v.containsKey('image')) {
 String image = v['image'].toString()
 image = image.replaceAll(VARIABLE, { m, x -> interpolateSingle(x as String) } )
 images[k.toString()] = image
 }
 }
 }
 if (only) {
 images = images.findAll { k,v -> only.contains(k) }
 }

 List model = []
 images.each { k, v -> model << [name: k, imageUri: v.toString()] }
 output.parentFile.mkdirs()
 output.text = JsonOutput.prettyPrint(JsonOutput.toJson(model))
 }

 protected String interpolateSingle(String variable) {
 if (!variable) {
 return ''
 }
 String[] split = variable.split(':-')
 String replacement = environment[split[0]]
 if (replacement == null) {
 replacement = (split.length > 1) ? split[1] : ''
 }
 replacement
 }
}

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ImageDefinitionsTaskSpec extends Specification {
 @Rule
 TemporaryFolder tmp = new TemporaryFolder()
 Project project

 void setup() {
 project = ProjectBuilder.builder().withProjectDir(tmp.newFolder()).build()
 }

 void "single compose file no environment"() {
 given:
 File testFile = tmp.newFile()
 project.tasks.create('imageDef', ImageDefinitionsTask) {
 output = testFile
 dockerComposeFiles = [new File('src/test/resources/imagedefinitions-compose1.yml')]
 }

 when:
 project.evaluate()
 project.tasks.imageDef.createOutput()

 then:
 new JsonSlurper().parse(testFile) == [
 [
 name: 'mongodb',
 imageUri: 'pdouble16/autopilotpattern-mongodb:3.6.3-r3',
 ],
 [
 name: 'api',
 imageUri: '/api:latest',
 ],
 [
 name: 'consul',
 imageUri: 'pdouble16/autopilotpattern-consul:1.0.6-r1',
 ]
 ]
 }

 void "single compose file with environment"() {
 given:
 File testFile = tmp.newFile()
 project.tasks.create('imageDef', ImageDefinitionsTask) {
 output = testFile
 dockerComposeFiles = [new File('src/test/resources/imagedefinitions-compose1.yml')]
 environment = [ PRIVATE_DOCKER_REGISTRY: 'gatekeeper:5001', APPLICATION_VERSION: '1.2.3' ]
 }

 when:
 project.evaluate()
 project.tasks.imageDef.createOutput()

 then:
 new JsonSlurper().parse(testFile) == [
 [
 name: 'mongodb',
 imageUri: 'pdouble16/autopilotpattern-mongodb:3.6.3-r3',
 ],
 [
 name: 'api',
 imageUri: 'gatekeeper:5001/api:1.2.3',
 ],
 [
 name: 'consul',
 imageUri: 'pdouble16/autopilotpattern-consul:1.0.6-r1',
 ]
 ]
 }

 void "multiple compose files"() {
 given:
 File testFile = tmp.newFile()
 project.tasks.create('imageDef', ImageDefinitionsTask) {
 output = testFile
 dockerComposeFiles = [
 new File('src/test/resources/imagedefinitions-compose1.yml'),
 new File('src/test/resources/imagedefinitions-compose2.yml')
 ]
 environment = [ PRIVATE_DOCKER_REGISTRY: 'gatekeeper:5001', APPLICATION_VERSION: '1.2.3' ]
 }

 when:
 project.evaluate()
 project.tasks.imageDef.createOutput()

 then:
 new JsonSlurper().parse(testFile) == [
 [
 name: 'mongodb',
 imageUri: 'pdouble16/autopilotpattern-mongodb:3.6.3-r3',
 ],
 [
 name: 'api',
 imageUri: 'gatekeeper:5001/api:1.2.3',
 ],
 [
 name: 'consul',
 imageUri: 'consul:latest',
 ]
 ]
 }

 void "single compose file with environment and subset of services"() {
 given:
 File testFile = tmp.newFile()
 project.tasks.create('imageDef', ImageDefinitionsTask) {
 output = testFile
 only << 'api'
 dockerComposeFiles = [new File('src/test/resources/imagedefinitions-compose1.yml')]
 environment = [ PRIVATE_DOCKER_REGISTRY: 'gatekeeper:5001', APPLICATION_VERSION: '1.2.3' ]
 }

 when:
 project.evaluate()
 project.tasks.imageDef.createOutput()

 then:
 new JsonSlurper().parse(testFile) == [
 [
 name: 'api',
 imageUri: 'gatekeeper:5001/api:1.2.3',
 ]
 ]
 }

}

