package com.ecommerce.kafkahighconcurrencyproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MerchantMasterDto {
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_name")
    private String clientname;

    @JsonProperty("merchant_state")
    private String merchantState;

    @JsonProperty("rvp_surcharges")
    private int rvpSurcharges;
}
