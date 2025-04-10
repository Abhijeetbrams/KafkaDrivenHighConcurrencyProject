
package com.ecommerce.kafkahighconcurrencyproject.util;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
@Log4j2
@ConditionalOnProperty(name = "enable.throttle", havingValue = "true")
public class ThrottleService {
    
    @Autowired
    ConfigProperty configProperty;
    
    @Autowired
    HttpServletRequest request;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    private KafkaTemplate<String, Object> template;
    
    public boolean pushInbound(InboundDto inboundDto) {
        try {
            template.send(configProperty.getInboundRequestTopic(),
                    inboundDto.getEndpointId(), inboundDto);
            return true;
        } catch (Exception e) {
            log.error("Exception pushInbound {}", e);
            return false;
        }
    }
    
    /**
     * Check if the request was sent via throttle service
     * @return 
     */
    public boolean isRequestFromThrottle() {
        try {
            String inboundId = request.getHeader("inbound-id");
            return inboundId != null && !inboundId.equals(AppConstant.EMPTY);
        } catch (Exception e) {
            log.error("isRequestFromThrottle: {}", e);
            return false;
        }
    }
    
    /**
     * Pushes the message to inbound for retry for a particular endpoint
     * @param endpointId
     * @param requestBody 
     */
    public void retry(String endpointId, Object requestBody) {
        try {
            if (isRequestFromThrottle()) {
                throw new AppRetryException();
            } else {
                InboundDto inboundDto = new InboundDto();
                inboundDto.setEndpointId(endpointId);
                inboundDto.setBody(objectMapper.writeValueAsString(requestBody));
                pushInbound(inboundDto);
            }
        } catch (JsonProcessingException jpe) {
            log.error("Retry Failure: {}", jpe);
        }
    }
}
