package com.ecommerce.kafkahighconcurrencyproject.kafka;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Autowired
    ConfigProperty configProperty;

    public ConsumerFactory<String, Object> consumerFactorySecondary(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        final JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();// ignore type headers which contain class name
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, configProperty.getEkartDBSchemaName() + ".rawdata.consumer");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new CompositeDeserializer());
    }
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        final JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();// ignore type headers which contain class name
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(),
                new CompositeDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(kafkaProperties));
        factory.setConsumerFactory(consumerFactory(kafkaProperties));
        factory.setConcurrency(2);
        factory.setBatchErrorHandler(new KafkaErrorHandler());
        // enable batch listening
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactorySecondary(
            KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactorySecondary(kafkaProperties));
        factory.setConsumerFactory(consumerFactorySecondary(kafkaProperties));
        factory.setConcurrency(2);
        factory.setBatchErrorHandler(new KafkaErrorHandler());
        // enable batch listening
        factory.setBatchListener(true);
        return factory;
    }

    public static class CompositeDeserializer implements Deserializer<Object> {
        private final Deserializer<Object> jsonStringDeserializer = new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Object.class));
        private final Deserializer<String> stringDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            jsonStringDeserializer.configure(configs, isKey);
            stringDeserializer.configure(configs, isKey);
        }

        @Override
        public Object deserialize(String topic, byte[] data) {
            try {
                // Try to deserialize as JSON
                return jsonStringDeserializer.deserialize(topic, data);
            } catch (Exception jsonException) {
                // If JSON deserialization fails, fall back to string deserialization
                return stringDeserializer.deserialize(topic, data);
            }
        }

        @Override
        public void close() {
            jsonStringDeserializer.close();
            stringDeserializer.close();
        }
    }

}

