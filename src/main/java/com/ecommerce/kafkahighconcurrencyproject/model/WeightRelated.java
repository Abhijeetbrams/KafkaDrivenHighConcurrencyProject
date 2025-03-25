package com.ecommerce.kafkahighconcurrencyproject.model;


import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "weight_related")
public class WeightRelated extends AbstractEntity {

    @Column(name ="client_id")
    private String clientId;

    @Column(name="reason")
    private String reason;
}
