
package com.ecommerce.kafkahighconcurrencyproject.util.log;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;

@Data
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TrafficLogs {

    private String id;
    private String orgId;
    private String serviceCode;
    private String url;
    private LinkedMultiValueMap<String, String> headers;
    private MultiValueMap<String, String> queryString;
    private HttpMethod requestMethod;
    private Long requestTime;
    private String requestBody;
    private Long responseTime;
    private String responseBody;
    private Integer statusCode;
    private String exception;
    private String carrierCode;
    private String clientIp;
    private String traceId;
    private boolean outbound;
    private String hostName;
    private String hostAddress;

}
