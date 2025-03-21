package com.ecommerce.kafkahighconcurrencyproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ClientStateMasterDto {
    @JsonProperty("client_code")
    private String clientCode;

    @JsonProperty("state")
    private List<String> state;

}
