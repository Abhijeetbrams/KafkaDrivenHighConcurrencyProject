package com.ecommerce.kafkahighconcurrencyproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StateMasterDto {
    @JsonProperty("state_name")
    private String stateName;

    @JsonProperty("state_code")
    private String stateCode;
}
