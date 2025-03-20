package com.ecommerce.kafkahighconcurrencyproject.dto;

import lombok.Data;

import java.util.List;

@Data
public class EkartRateDto {
    private String UUID;
    private BreakUp breakUp;
    private String errorMessage;
    private RateDto rateCalculationProcessRequestDTO;
    private double totalAmount;

    @Data
    public static class RateDto {
        private String companyId;
        private String flowCode;
        private String processId;
        private ProcessData processMasterCode;
        private String referenceNumber;
        private String timeZone;
        private String userId;
    }

    @Data
    public static class BreakUp {
        private double freightCharge;
        private List<AdditionalCharges> additionalCharges;

        @Data
        public static class AdditionalCharges {
            private double amount;
            private String chargeHead;
        }
    }
}
