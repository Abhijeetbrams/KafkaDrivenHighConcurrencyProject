package com.ecommerce.kafkahighconcurrencyproject.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name="discount_master")
public class DiscountMaster extends AbstractEntity {

    @Column(name="discount_client_id")
    private String clientId;

    @Column(name ="cod_discount_percentage")
    private double codDiscountPercentage;

    @Column(name ="freight_discount_percentage")
    private double freightDiscountPercentage;

    @Column(name ="applied_on")
    private String appliedOn;

    @Column(name = "activation_status")
    private Boolean activationStatus;


}
