package com.ecommerce.kafkahighconcurrencyproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RateCalculationRequestDTO {
    private String party;
    private String origin;
    private String destination;
    private boolean sameCity;
    private RateCalculationProcessRequestDTO rateCalculationProcessRequestDTO = new RateCalculationProcessRequestDTO();
    private String orderTypeCode;
    private String bookingDate;
    private int itemValue;
    private int codAmount;
    private int weight;
    private double storageDays;
    private String responseTopic;
    private List<String> additionalHeads = new ArrayList<>();
    @JsonProperty("service_type")
    private String serviceType;


    @Data
    public static class RateCalculationProcessRequestDTO {
        private String referenceNumber;
        private int companyId;
        private String timeZone;
        private String flowCode;
        private ProcessData processMasterCode;

    }


}
