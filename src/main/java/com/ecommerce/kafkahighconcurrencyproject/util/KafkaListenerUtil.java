
package com.ecommerce.kafkahighconcurrencyproject.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Log4j2
public class KafkaListenerUtil {

    private final KafkaListenerContainerFactory kafkaListenerContainerFactory;
    private final Map<String, MessageListenerContainer> registeredTopicMap;
    private final Object lock;

    public KafkaListenerUtil(final KafkaListenerContainerFactory kafkaListenerContainerFactory) {
        Assert.notNull(kafkaListenerContainerFactory, "kafkaListenerContainerFactory must be not null.");

        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.registeredTopicMap = new ConcurrentHashMap<>();
        this.lock = new Object();
    }

    /**
     * Kafka listener registration at runtime.*
     * @param topicSupplier
     * @param messageListenerSupplier
     */
    public void register(final Set<String> topics, final Supplier<GenericMessageListener> messageListenerSupplier,
                         final String consumerName, final String status) {
        Assert.notNull(topics, "topics must be not null.");
        Assert.notNull(messageListenerSupplier, "messageListenerSupplier must be not null.");

        synchronized (lock) {
            final Set<String> registeredTopics = getRegisteredTopics();

            if (topics.isEmpty()) {
                return;
            }

            topics.stream()
                    .filter(topic -> !registeredTopics.contains(topic))
                    .forEach(topic -> {
                        doRegister(topic, messageListenerSupplier.get(), consumerName, status);
                    });
        }
    }

    /**
     * Kafka listener de-registration at runtime.*
     * @param topicSupplier
     */
    public void deRegister(final Supplier<Set<String>> topicSupplier) {
        Assert.notNull(topicSupplier, "topicSupplier must be not null.");

        synchronized (lock) {
            final Set<String> registeredTopics = getRegisteredTopics();
            final Set<String> topics = topicSupplier.get();

            if (topics.isEmpty()) {
                return;
            }

            topics.stream()
                    .filter(registeredTopics::contains)
                    .forEach(this::doDeregister);
        }
    }

    /**
     * Kafka listener start at runtime *
     */
    public void start() {
        synchronized (lock) {
            final Collection<MessageListenerContainer> registeredMessageListenerContainers = getRegisteredMessageListenerContainers();
            registeredMessageListenerContainers.forEach(container -> {
                if (container.isRunning()) {
                    return;
                }
                container.start();
            });
        }
    }

    /**
     * Kafka listener stop at runtime *
     */
    public void stop() {
        synchronized (lock) {
            final Collection<MessageListenerContainer> registeredMessageListenerContainers = getRegisteredMessageListenerContainers();
            registeredMessageListenerContainers.forEach(container -> {
                if (!container.isRunning()) {
                    return;
                }
                container.stop();
            });
        }
    }

    public Map<String, MessageListenerContainer> getRegisteredTopicMap() {
        return Collections.unmodifiableMap(registeredTopicMap);
    }

    private void doRegister(final String topic, final GenericMessageListener messageListener,
                            final String consumerName, final String status) {
        Assert.hasLength(topic, "topic must be not empty.");
        Assert.notNull(messageListener, "messageListener must be not null.");

        final MessageListenerContainer messageListenerContainer = kafkaListenerContainerFactory.createContainer(topic);
        messageListenerContainer.setupMessageListener(messageListener);

        if(AppConstant.Container.START.name().equalsIgnoreCase(status)) {
            log.info("Starting container at startup for the {}",consumerName);
            messageListenerContainer.start();
        }

        registeredTopicMap.put(topic, messageListenerContainer);
    }

    private void doDeregister(final String topic) {
        Assert.hasLength(topic, "topic must not be empty.");

        final MessageListenerContainer messageListenerContainer = registeredTopicMap.get(topic);
        messageListenerContainer.stop();

        registeredTopicMap.remove(topic);
    }

    private Set<String> getRegisteredTopics() {
        return registeredTopicMap.keySet();
    }

    private Collection<MessageListenerContainer> getRegisteredMessageListenerContainers() {
        return registeredTopicMap.values();
    }

    /**
     * This method will pause consumer based on topic name.
     *
     * @param topic - The topic name.
     */
    public void pauseConsumer(String topic) {
        registeredTopicMap.get(topic).pause();
    }

    /**
     * This method will resume consumer based on topic name.
     *y
     * @param topic - The topic name.
     */
    public void resumeConsumer(String topic) {
        registeredTopicMap.get(topic).resume();
    }

    /**
     * This method will resume consumer based on topic name.
     *y
     * @param topic - The topic name.
     */
    public void stopConsumer(String topic) {
        registeredTopicMap.get(topic).stop();
    }
}
