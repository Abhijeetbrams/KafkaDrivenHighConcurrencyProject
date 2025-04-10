
package com.ecommerce.kafkahighconcurrencyproject.util.log;

import co.fareye.config.ConfigProperty;
import co.fareye.config.security.GatewayUser;
import co.fareye.constant.AppConstant.ServiceCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.UUID;
import java.util.concurrent.Future;


@Service
@Log4j2
public class TrafficLogsService {

    @Autowired
    @Qualifier("StringKeyValueProducer")
    private KafkaProducer<String, String> kafkaProducer;

    @Autowired
    private ConfigProperty configProperty;

    @Autowired
    private ObjectMapper objectMapper;

//    @Autowired
//    Tracer tracer;

    /**
     * @param url
     * @param requestData   Received request data
     * @param responseData  Response data
     * @param requestTime   Request time when request is trigger.
     * @param responseTime  Reponse time when response has sent.
     * @param statusCode    Status code request like 200, 400 or 500 etc..
     * @param exception     Catch the exception if occurred.
     * @param ipAddress     ip address where request has been made.
     * @param headers
     * @param queryParams
     * @param reqComplete
     * @param requestMethod
     */
    public void log(String url, String requestData, String responseData, Long requestTime, Long responseTime,
                    int statusCode, String exception, String ipAddress, MultiValueMap<String, String> headers,
                    MultiValueMap<String, String> queryParams, HttpMethod requestMethod, boolean outbound) {
        String orgId = GatewayUser.getUser() == null ? "1" : GatewayUser.getUser().getOrganizationId();
        ServiceCode serviceCode = ServiceDetails.getServiceCode();// set by dev or from url
//        TrafficLogs trafficLogs = TrafficLogs.builder().carrierCode("")
//                .orgId(configProperty.getOrganizationId().isEmpty() ? orgId : configProperty.getOrganizationId())
//                .clientIp(ipAddress).url(url).headers((LinkedMultiValueMap<String, String>) headers)
//                .exception(exception).queryString(queryParams).responseTime(responseTime).statusCode(statusCode)
//                .responseBody(responseData).requestTime(requestTime).requestMethod(requestMethod)
//                .requestBody(requestData).serviceCode(serviceCode != null ? serviceCode.toString() : "")
//                .traceId(getTraceId()).outbound(outbound).build();
//
//        trafficLogs.setCarrierCode(ServiceDetails.getCarrierCode());
//        insertTrafficLogs(trafficLogs);
    }

    /**
     * Insert into traffic logs table.
     *
     * @param trafficLogs
     */
    public void insertTrafficLogs(TrafficLogs trafficLogs) {
        try {
            pushLogsToTopic(trafficLogs);
        } catch (Exception e) {
            log.error("Exception insertTrafficLogs {}", e);
        }
    }

    @Async
    public boolean pushLogsToTopic(TrafficLogs trafficLogs) {
        try {
            String value = objectMapper.writeValueAsString(trafficLogs);
            String key = UUID.randomUUID().toString();
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(configProperty.getTrafficLogTopic(),
                    key, value);
            Future<RecordMetadata> futureRecord = kafkaProducer.send(producerRecord);
            log.info(key + " " + futureRecord.get().toString());
            if (!futureRecord.isDone()) {
                log.error("KafkaPushFailed {}:", futureRecord.get().toString());
                return false;
            } else {
                log.info("KafkaPushSuccess");
                return true;
            }
        } catch (Exception e) {
            log.error("Exception pushLogsToTopic {}", e);
            return false;
        }
    }

    /**
     * Get Trace Id of current request
     *
     * @return
     */
//    private String getTraceId() {
//        if (tracer.activeSpan() != null) {
//            SpanContext spanContext = tracer.activeSpan().context();
//            if (spanContext != null) {
//                return spanContext.toTraceId();
//            }
//        }
//        return null;
//    }
}
