package com.lxy.kafka.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Slf4j
public class KafkaDelayedQueue {

    /**
     * kafka服务器地址,可以指定多个，以逗号分割
     */
    private String bootstrapServers;

    /**
     * 延迟时间，单位毫秒
     */
    private int delayMs;

    /**
     * 队列名称
     */
    private String topic;

    /**
     * 分区数
     */
    private int partitions;

    /**
     * 事务前缀
     */
    private String transactionPrefix;

    /**
     * 内部组名，对外不可见
     */
    private String monitorTopicName;

    /**
     * 内部topic名，对外不可见
     */
    private String monitorGroupName;

    /**
     * 事务ID后缀
     */
    private AtomicInteger transactionSuffix = new AtomicInteger(0);

    /**
     * 事务producer缓存
     */
    private BlockingQueue<Producer<String,String>> cache;

    /**
     * ready任务offset存储
     */
    private SaveRestoreOffset readySaveRestoreOffset;


    private String offsetReset;

    private int maxPollRecords;

    private boolean readyTopicExist = false;
    private boolean monitorTopicExist = false;
    private boolean runTimeHook = false;

    private static  ObjectMapper objectMapper = new ObjectMapper();

    private static KafkaProducer<String,String> monitorProducer = null;

    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private final AtomicBoolean runningConsumer = new AtomicBoolean(true);


    /**
     * 延迟任务topic名前缀
     */
    private static final String MQ_DELAY_TOPIC_PREFIX = "MQ_DELAY_TOPIC_";

    /**
     * 延迟任务消费组前缀
     */
    private static final String MQ_DELAY_GROUP_PREFIX = "MQ_DELAY_GROUP_";

    /**
     * 默认事务前缀
     */
    private static final String DEFAULT_TX_PREFIX = "TX";

    /**
     * 默认分区数
     */
    private static final int DEFAULT_PARTITIONS_NUMBER = 12;

    public static final String OFFSET_RESET_LATEST = "latest";

    public static final String OFFSET_RESET_EARLIEST = "earliest";


    public KafkaDelayedQueue(MqDelayedConfig config) {

        this.bootstrapServers = config.bootstrapServers;
        this.topic = config.topic;
        this.delayMs = config.delayMs;
        this.partitions = config.partitions <= 0 ? DEFAULT_PARTITIONS_NUMBER : config.partitions;
        this.transactionPrefix = isBlank(config.transactionPrefix) ? DEFAULT_TX_PREFIX : config.transactionPrefix;
        this.readySaveRestoreOffset = config.readySaveRestoreOffset == null ? new MemorySaveRestoreOffset() : config.readySaveRestoreOffset;
        this.offsetReset = isBlank(config.offsetReset) ? OFFSET_RESET_EARLIEST : config.offsetReset;
        this.maxPollRecords = config.maxPollRecords <= 0 ? 1 : config.maxPollRecords;
        this.cache = new LinkedBlockingQueue<>(partitions);

        this.monitorGroupName = getGroupName(delayMs, topic);
        this.monitorTopicName = getTopicName(delayMs, topic);
    }

    private boolean isBlank(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * 组装topic名字
     * @param delayMs 延迟时间
     * @param topic topic名
     * @return 组装后的名字
     */
    private static String getTopicName(long delayMs, String topic) {
        return String.format("%s%s_%d", MQ_DELAY_TOPIC_PREFIX, topic, delayMs);
    }

    /**
     * 组装组名
     * @param delayMs 迟延时间
     * @param group 组名
     * @return 组装后的组名
     */
    private static String getGroupName(long delayMs, String group) {
        return String.format("%s%s_%d", MQ_DELAY_GROUP_PREFIX, group, delayMs);
    }

    /**
     * 监控服务退出
     */
    private void doRunTimeHook() {
        if(!runTimeHook) {
            synchronized (this) {
                if(!runTimeHook) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        runningConsumer.set(false);
                        while (true) {
                            Producer<String,String> producer = cache.poll();
                            if(producer == null) {
                                break;
                            }
                            producer.close(1, TimeUnit.SECONDS);
                        }
                    }));
                    runTimeHook = true;
                }
            }
        }
    }

    private boolean ensureTopic(String topic) {

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);

        log.info("start create topic: {}", topic);

        try(KafkaAdminClient adminClient = (KafkaAdminClient) AdminClient.create(props))  {

            Map<ConfigResource, Config> configMap = new HashMap<>(1);
            ConfigResource topicConfig = new ConfigResource(ConfigResource.Type.TOPIC, topic);
            String value = String.valueOf(delayMs + (Duration.ofDays(5).toMillis()));

            // 配置日志过期时间
            configMap.put(topicConfig,new Config(Collections.singleton(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, value))));

            try {
                adminClient.createTopics(Collections.singleton(new NewTopic(topic, partitions, (short) 1))).all().get();
                adminClient.alterConfigs(configMap).all().get();
            } catch (ExecutionException e) {
                if(e.getCause() instanceof  TopicExistsException) {
                    log.warn("topic: {} already exists,ignore create", topic);
                } else {
                    log.error("create topic: {} error", topic, e);
                    return false;
                }
            } catch(InterruptedException e) {
                log.error("create topic: {} error", topic, e);
                return false;
            }
        }
        return true;
    }

    private Producer<String,String> getMonitorProducer() {
        if(monitorProducer == null) {
            synchronized (this) {
                if(monitorProducer == null) {
                    String name = "MQ_DELAY_MONITOR_PRODUCER";
                    Properties props = new Properties();
                    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    props.put(ProducerConfig.CLIENT_ID_CONFIG, name);
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    monitorProducer = new KafkaProducer<>(props);
                }
            }
        }
        return monitorProducer;
    }

    private Producer<String,String> createTransactionProducer() {
        Producer<String,String> transactionProducer = cache.poll();
        if(transactionProducer == null) {
            int suffix = transactionSuffix.incrementAndGet();
            String name = "MQ_DELAY_READY_PRODUCER";
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.CLIENT_ID_CONFIG, topic + "_" + name + "_" + suffix);
            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
            props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionPrefix + "_" + topic + "_" + name + "_" + suffix);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            transactionProducer = new KafkaProducer<>(props);
            transactionProducer.initTransactions();
        }
        return transactionProducer;
    }

    private void closeTransactionProducer(Producer<String,String> transactionProducer) {
        if(transactionProducer != null) {
            this.cache.offer(transactionProducer);
        }
    }

    private void sendReadyTopic(Collection<InternalDelayedRecord> records)  {

        log.info("send ready record: {}", records);

        Producer<String,String> transactionProducer = createTransactionProducer();

        try {
            // 开始事务
            transactionProducer.beginTransaction();
            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>(records.size());
            for(InternalDelayedRecord record : records) {
                TopicPartition tp = new TopicPartition(record.getTopic(), record.getPartition());
                OffsetAndMetadata om = offsets.get(tp);
                if(om == null || om.offset() < record.getOffset() + 1) {
                    offsets.put(tp, new OffsetAndMetadata(record.getOffset() + 1));
                }
                transactionProducer.send(new ProducerRecord<>(topic, record.getMsg().getKey(), record.getMsg().getBody()));
            }
            transactionProducer.sendOffsetsToTransaction(offsets, monitorGroupName);

            // 提交事务
            transactionProducer.commitTransaction();
        } catch (KafkaException e) {
            Throwable cause = e.getCause();
            if(cause instanceof ProducerFencedException || cause instanceof OutOfOrderSequenceException || cause instanceof AuthorizationException ) {
                // ProducerFencedException 表示有新的实例transaction.id与当前实例相同，并且epoch比当前要新，这是一个fatal错误，必须close
                // OutOfOrderSequenceException 表示发送的数据出现乱序，如果当前的producer是幂等性并且开启了事务，那这就是一个fatal错误，必须close
                log.error("producer ready msg error",e);
                transactionProducer.abortTransaction();
                transactionProducer.close(1, TimeUnit.SECONDS);
                transactionProducer = null;
            } else {
                transactionProducer.abortTransaction();
            }
        } finally {
            closeTransactionProducer(transactionProducer);
        }
    }

    private void createMonitorConsumer(int threadCount) {

        String topicName = monitorTopicName;
        String groupName = monitorGroupName;

        int maxPollRecords = 50;

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupName);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // 重要，表示最多一次拉出的消息条数
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // 心跳时间，当服务器在此时间内没有收到
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);

        // 当发生rebalance此设置可以加快rebalance时间,一般设置为 SESSION_TIMEOUT 的 1/3
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // 重要,业务逻辑最长处理时间，业务线程
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, delayMs + 2000);

        // 此值必须大于MAX_POLL_INTERVAL_MS_CONFIG
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, delayMs + 5000);

        for(int i = 0; i < threadCount; ++i) {

            new Thread(() -> {
                final Consumer<String, String> consumer = new KafkaConsumer<>(props);
                Set<InternalDelayedRecord> delayedRecords = new LinkedHashSet<>(maxPollRecords);
                List<InternalDelayedRecord> readyRecords = new ArrayList<>(maxPollRecords);


                consumer.subscribe(Collections.singletonList(topicName), new ConsumerRebalanceListener() {

                    //This callback will only execute in the user thread as part of the poll(long) call whenever partition assignment changes.

                    /**
                     * A callback method the user can implement to provide handling of offset commits to a customized store on the start of a rebalance operation.
                     * This method will be called before a rebalance operation starts and after the consumer stops fetching data.
                     * It is recommended that offsets should be committed in this callback to either Kafka or a custom offset store to prevent duplicate data.
                     * @param collection
                     */
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                        delayedRecords.clear();
                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> collection) {

                    }
                });

                do {
                    try {
                        ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(100));
                        long now = System.currentTimeMillis();

                        if(!records.isEmpty()) {
                            for (ConsumerRecord<String, String> record : records) {
                                MqDelayedMsg msg = null;
                                try {
                                    msg = objectMapper.readValue(record.value(), MqDelayedMsg.class);
                                } catch (IOException e) {
                                    log.error("decode json error", e);
                                }
                                if (msg != null) {

                                    InternalDelayedRecord delayedRecord = new InternalDelayedRecord();
                                    delayedRecord.setMsg(msg);
                                    delayedRecord.setTopic(record.topic());
                                    delayedRecord.setOffset(record.offset());
                                    delayedRecord.setPartition(record.partition());

                                    if(msg.getDelayed() - now <= 0) {
                                        readyRecords.add(delayedRecord);
                                    } else {
                                        log.info("wait delay: {}", delayedRecord);
                                        delayedRecords.add(delayedRecord);
                                        // 执行pause后poll可以正常调用，但不会返回数据
                                        consumer.pause(Collections.singletonList(new TopicPartition(record.topic(), record.partition())));
                                    }
                                }
                            }
                        }

                        if(!delayedRecords.isEmpty()) {
                            // 记录哪些分区需要恢复消费
                            Map<Integer, Integer> resumePartitions = new HashMap<>(maxPollRecords);

                            Iterator<InternalDelayedRecord> it = delayedRecords.iterator();

                            while (it.hasNext()) {
                                InternalDelayedRecord delayedRecord = it.next();
                                Integer v = resumePartitions.merge(delayedRecord.getPartition(), 1, (a, b) -> a + b);
                                MqDelayedMsg msg = delayedRecord.getMsg();
                                if (msg.getDelayed() - now <= 0) {
                                    resumePartitions.put(delayedRecord.getPartition(), v - 1);
                                    readyRecords.add(delayedRecord);
                                    it.remove();
                                }
                            }

                            resumePartitions.forEach((k, v) -> {
                                if(v == 0) {
                                    try {
                                        // 假如当前分区已经没有等待处理的延迟任务，恢复消费
                                        consumer.resume(Collections.singletonList(new TopicPartition(topicName, k)));
                                    } catch (Throwable e) {
                                        log.info("consumer.resume exception", e);
                                    }
                                }
                            });
                        }

                        if(!readyRecords.isEmpty()) {
                            sendReadyTopic(readyRecords);
                            readyRecords.clear();
                        }

                    } catch (Throwable throwable) {
                        log.error("catch exception:", throwable);
                    }
                } while (runningConsumer.get());
                consumer.close();
            }, String.format("%s-delayed-consumer-%d", topic, threadCounter.incrementAndGet())).start();
        }
    }

    public void createReadyConsumer(int threadCount, String groupId, int maxCbProcessMs, BiConsumer<String,String> callback) {

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // 心跳时间，当服务器在此时间内没有收到就会触发rebalance
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);

        // 当发生rebalance此设置可以加快rebalance时间,一般设置为 SESSION_TIMEOUT 的 1/3
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        maxCbProcessMs = maxCbProcessMs < 30000 ? 30000 : maxCbProcessMs;

        // 业务逻辑最长处理时间，业务线程
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxCbProcessMs);

        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, (int)(maxCbProcessMs + Duration.ofSeconds(2).toMillis()));

        for(int i = 0;i < threadCount; ++i) {
            new Thread(() -> {
                final Consumer<String, String> consumer = new KafkaConsumer<>(props);
                Map<TopicPartition, OffsetAndMetadata> tpo = new HashMap<>();
                Map<TopicPartition, OffsetAndMetadata> commit = new HashMap<>();

                consumer.subscribe(Collections.singletonList(topic));

                consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        // 这里保存offset
                        readySaveRestoreOffset.saveOffset(consumer, tpo);
                        tpo.clear();
                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        // 这里恢复offset
                        Map<TopicPartition, OffsetAndMetadata> offsets = readySaveRestoreOffset.restoreOffset(consumer, partitions);
                        offsets.forEach((t, o) -> consumer.seek(t, o.offset()));
                    }
                });

                do {
                    try {
                        ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(100));
                        if(!records.isEmpty()) {
                            for(ConsumerRecord<String, String> record : records) {
                                readySaveRestoreOffset.beforeProcess(consumer, records);
                                if(callback != null) {
                                    try {
                                        String key = record.key();
                                        String value = record.value();
                                        callback.accept(key, value);
                                        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                                        OffsetAndMetadata om = new OffsetAndMetadata(record.offset()+1);

                                        // 同一个tp只保存最后一个offset metadata
                                        tpo.put(tp, om);

                                        // 待提交的offset
                                        commit.put(tp, om);

                                    } catch (Throwable e) {
                                        log.error("callback ready queue error: key: {}, value: {}", record.key(), record.value(), e);
                                    }
                                }
                                readySaveRestoreOffset.afterProcess(consumer, records);
                            }
                            try {
                                consumer.commitSync(commit);
                                commit.clear();
                            } catch (Throwable ex) {
                                // 提交失败，将offset存起来
                                readySaveRestoreOffset.saveOffset(consumer, tpo);
                            }
                        }
                    } catch (Throwable e) {
                        log.error("catch error", e);
                    }
                } while (runningConsumer.get());
                consumer.close();
            }, String.format("ready-task-consumer-%d", threadCounter.incrementAndGet())).start();
        }
    }

    /**
     * 启动消费延迟队列
     * @param groupId 消费组，与kafka消费组定义一致
     * @param maxCbProcessMs 业务最长处理时间
     * @param callback 延迟任务回调
     */
    public void start(String groupId, int maxCbProcessMs, BiConsumer<String,String> callback) throws DelayedQueueException {
        start(groupId, 1, maxCbProcessMs, callback);
    }

    /**
     * 启动消费延迟队列
     * @param groupId 消费组，与kafka消费组定义一致
     * @param threadCount 线程数，注意，此线程数不能超过topic分区数
     * @param maxCbProcessMs 业务最长处理时间
     * @param callback 延迟任务回调
     */
    public void start(String groupId, int threadCount, int maxCbProcessMs, BiConsumer<String,String> callback) throws DelayedQueueException {
        if(!readyTopicExist) {
            synchronized (this) {
                if(!readyTopicExist) {
                    if(ensureTopic(topic)) {
                        readyTopicExist = true;
                    }
                }
            }
        }
        if(!readyTopicExist) {
            throw new DelayedQueueException(String.format("创建%s出错",topic));
        }
        doRunTimeHook();
        createMonitorConsumer(threadCount);
        createReadyConsumer(threadCount, groupId, maxCbProcessMs, callback);
    }

    public void start(BiConsumer<String,String> callback) throws DelayedQueueException {
        start(topic, 1, 0, callback);
    }

    /**
     * 向延迟队列添加任务
     * @param key key,与kafka的key一致
     * @param value value,任务内容,自定义
     */
    public void offer(String key, String value, Callback cb) throws DelayedQueueException{
        String topicName = monitorTopicName;
        if(!monitorTopicExist) {
            synchronized (this) {
                if(!monitorTopicExist) {
                    if(ensureTopic(topicName)) {
                        monitorTopicExist = true;
                    }
                }
            }
        }
        if(!monitorTopicExist) {
            throw new DelayedQueueException(String.format("创建%s出错", topicName));
        }

        MqDelayedMsg msg = new MqDelayedMsg(System.currentTimeMillis() + delayMs, key, value );
        try {
            log.info("send origin msg: {}", msg);
            getMonitorProducer().send(new ProducerRecord<>(topicName, key, objectMapper.writeValueAsString(msg)), cb);
        } catch (JsonProcessingException e) {
            log.error("send msg error", e);
        }
    }

    public void offer(String key, String value) throws DelayedQueueException{
        offer(key, value, null);
    }


    @Data
    @Builder
    public static class MqDelayedConfig {
        /**
         * REQUIRED kafka服务器地址
         */
        private String bootstrapServers;

        /**
         * REQUIRED 队列名称
         */
        private String topic;

        /**
         * REQUIRED 延迟时间,单位毫秒,注意，有一定的误差
         */
        private int delayMs;


        /**
         * optional 分区数,默认12
         */
        private int partitions;

        /**
         * optional 事务前缀,默认TX，注意，当启动多个生产者的时候需要配置此选项，必须保证每个生产者的前缀不同
         *
         */
        private String transactionPrefix;

        /**
         * optional 当发生rebalance时保存offset跟恢复offset,主要用于处理重复消费
         */
        private String offsetReset;
        private int maxPollRecords;
        private SaveRestoreOffset readySaveRestoreOffset = null;
    }

    @ToString
    @Data
    @EqualsAndHashCode
    @NoArgsConstructor // for json construct
    public static class MqDelayedMsg {
        private long delayed;
        private String key;
        private String body;

        public MqDelayedMsg(long delayed, String key, String body) {
            this.delayed = delayed;
            this.key = key;
            this.body = body;
        }
    }

    @ToString
    @Data
    @EqualsAndHashCode
    @NoArgsConstructor // for json construct
    public static class InternalDelayedRecord {
        private MqDelayedMsg msg;
        private String topic;
        private long offset;
        private int partition;
    }

    public static class DelayedQueueException extends Exception {
        public DelayedQueueException(String msg) {
            super(msg);
        }
    }

    public interface SaveRestoreOffset {

        /**
         * 消费者拉取完数据，丢给业务处理之前回调
         * @param records
         */
        default void beforeProcess(Consumer<String,String> consumer, ConsumerRecords<String,String> records) {}

        /**
         * 业务处理完数据后回调
         * @param records
         */
        default void afterProcess(Consumer<String,String> consumer, ConsumerRecords<String,String> records) {}

        /**
         * 当发生Rebalance之前调用
         * @param offset 还没有提交上去的offset
         */
        void saveOffset(Consumer<String,String> consumer, Map<TopicPartition, OffsetAndMetadata> offset);

        /**
         * rebalance之后调用
         * @param tps 当前消费者分配的的分区
         * @return
         */
        Map<TopicPartition, OffsetAndMetadata> restoreOffset(Consumer<String,String> consumer, Collection<TopicPartition> tps);
    }

    public static class MemorySaveRestoreOffset implements SaveRestoreOffset {

        private Lock lock = new ReentrantLock();
        private Map<TopicPartition, OffsetAndMetadata> saveOffsets = new HashMap<>();
        

        @Override
        public void saveOffset(Consumer<String,String> consumer, Map<TopicPartition, OffsetAndMetadata> offset) {
            try {
                lock.lock();
                saveOffsets.putAll(offset);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Map<TopicPartition, OffsetAndMetadata> restoreOffset(Consumer<String,String> consumer, Collection<TopicPartition> tps) {
            try {
                lock.lock();
                Map<TopicPartition, OffsetAndMetadata> result = new HashMap<>(saveOffsets.size());
                for(TopicPartition tp : tps) {
                    OffsetAndMetadata o = saveOffsets.get(tp);
                    if(o != null) {
                        result.put(tp, o);
                    }
                }
                return result;
            } finally {
                lock.unlock();
            }
        }
    }
}
