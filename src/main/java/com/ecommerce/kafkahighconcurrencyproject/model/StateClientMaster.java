package com.ecommerce.kafkahighconcurrencyproject.model;


import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name="state_client_tax_master")
public class StateClientMaster extends AbstractEntity {

    @Column(name="client_code")
    private String clientCode;

    @Column(name = "state")
    private String state;
}
