package com.ecommerce.kafkahighconcurrencyproject.model;


import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "merchant_master_non_large")
public class MerchantMaster extends AbstractEntity {

    @Column(name ="client_id")
    private String clientId;

    @Column(name="client_name")
    private String clientName;

    @Column(name="merchant_state")
    private String merchantState;

    @Column(name="rvp_surcharges")
    private int  rvpSurcharges;
}
