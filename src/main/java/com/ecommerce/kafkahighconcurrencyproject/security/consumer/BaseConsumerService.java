
package com.ecommerce.kafkahighconcurrencyproject.security.consumer;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.BatchMessageListener;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@Log4j2
public abstract class BaseConsumerService {

    @Autowired
    public ConfigProperty configProperty;

    protected KafkaListenerUtil kafkaListenerUtil;

    private ConcurrentKafkaListenerContainerFactory factory;

    private KafkaListenerContainerFactory kafkaListenerContainerFactory;

    private String consumerName;

    private Set<String> currentTopic;

    private int concurrency = 1;

    private String status = "START";

    /**
     * Construct an instance with the supplied configuration properties.
     * on initialization only make sure the respective consumer have it's concurrency assigned
     * and store details like topic , concurrency and status at global scope
     * @param kafkaListenerContainerFactory the consumer factory.
     * @param consumerName for the consumer name.
     */
    public BaseConsumerService(final KafkaListenerContainerFactory kafkaListenerContainerFactory,
                               final String consumerName, final String topic,
                               final Integer concurrency, final String status)  {

        setConsumerName(consumerName);
        this.currentTopic = Collections.singleton(topic);
        this.status = status;
        this.concurrency = concurrency;
        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.factory = (ConcurrentKafkaListenerContainerFactory)kafkaListenerContainerFactory;
        this.factory.setConcurrency(this.concurrency);
        this.kafkaListenerUtil = new KafkaListenerUtil(factory);
    }

    private void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    @PostConstruct
    public void init() {
        this.kafkaListenerUtil.register(this.currentTopic, this::messageListener, this.consumerName, this.status);
    }

    private BatchMessageListener<String, Object> messageListener() {
        return record -> log.info("Message Consumed: {}", consumeMessage(record));
    }


    /**
     * .Override in consumers inheriting, and calling refresh containers method
     */
    @EventListener(RefreshScopeRefreshedEvent.class)
    public abstract void onRefresh(RefreshScopeRefreshedEvent event);

    /**
     * .On refresh event, need to create the containers with new concurrency if changed,
     *  new topic and the status.
     */
    public void refreshContainers(int concurrency, Set<String> topics, String status) {
        if(didChange(concurrency, topics)) {
            this.kafkaListenerUtil.deRegister(() -> this.currentTopic);
            changeConcurrency(concurrency);
            this.kafkaListenerUtil.register(this.topicSet(topics), this::messageListener, this.consumerName, status);
        } else if(!this.status.equalsIgnoreCase(status)){
            changeStatus(status, topics);
        }

        this.status = status;
    }

    private boolean didChange(int concurrency, Set<String> topics) {
        return !this.currentTopic.equals(topics) || (concurrency >= 1 && concurrency != this.concurrency);
    }

    private Set<String> topicSet(Set<String> topics) {
        log.info("Bean is being refreshed... {}", Arrays.toString(topics.toArray()));
        currentTopic = topics;
        return topics;
    }

    private void changeConcurrency(int concurrency) {
        this.concurrency = concurrency;
        this.factory = (ConcurrentKafkaListenerContainerFactory)this.kafkaListenerContainerFactory;
        this.factory.setConcurrency(this.concurrency);
        this.kafkaListenerUtil = new KafkaListenerUtil(this.factory);
    }

    private void changeStatus(String status, Set<String> topics) {
        log.info("{} container on refresh for the {}", AppConstant.Container.valueOf(status), this.consumerName);
        this.kafkaListenerUtil.deRegister(() -> this.currentTopic);
        this.kafkaListenerUtil.register(this.topicSet(topics), this::messageListener, this.consumerName, status);
    }


    /**
     * Override in consumers inheriting, with it's consumer logic.
     *
     */
    public abstract boolean consumeMessage(List<ConsumerRecord<String, Object>> list);

}