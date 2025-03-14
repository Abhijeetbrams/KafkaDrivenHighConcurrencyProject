package com.ecommerce.kafkahighconcurrencyproject.kafka;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.ConsumerAwareBatchErrorHandler;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class KafkaErrorHandler implements ConsumerAwareBatchErrorHandler {

    @Autowired
    ConfigProperty configProperty;

    @Override
    public void handle(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer) {
        log.info("Exception occured while processing::" + thrownException.getMessage());
        log.info("Consumer Records" + data.records(configProperty.getEkartRateTopic()).toString());
        try {
            String s = thrownException.getMessage().split("Error deserializing key/value for partition ")[1]
                    .split(". If needed, please seek past the record to continue consumption.")[0];

            // modify below logic according to your topic nomenclature
            String topics = s.substring(0, s.lastIndexOf('-'));
            int offset = Integer.parseInt(s.split("offset ")[1]);
            int partition = Integer.parseInt(s.substring(s.lastIndexOf('-') + 1).split(" at")[0]);

            TopicPartition topicPartition = new TopicPartition(topics, partition);
            log.info("Skipping {} - {} offset {}", topics, partition, offset);
            consumer.seek(topicPartition, offset + 1);

            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
            offsets.put(new TopicPartition(topics, partition), new OffsetAndMetadata(offset + 1));
            // @Todo - will add DLQ flow here
            consumer.commitSync(offsets);
        } catch (Exception e) {
            log.error("Kafka Unknown Exception", e);
        }

    }
}