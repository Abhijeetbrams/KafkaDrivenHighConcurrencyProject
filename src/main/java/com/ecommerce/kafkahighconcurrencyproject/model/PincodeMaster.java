/**
 * ****************************************************************************
 *
 * Copyright (c) 2024, FarEye and/or its affiliates. All rights
 * reserved.
 * ___________________________________________________________________________________
 *
 *
 * NOTICE: All information contained herein is, and remains the property of
 * FarEye and its suppliers,if any. The intellectual and technical concepts
 * contained herein are proprietary to FarEye. and its suppliers and
 * may be covered by us and Foreign Patents, patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from FarEye.
 */
package com.ecommerce.kafkahighconcurrencyproject.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author Shubham Panth
 */
@Data
@Entity
@Table(name = "`pincode_master`", catalog = "ekart-db")
public class PincodeMaster extends AbstractEntity {

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "city")
    private String city;

    @Column(name = "zstate")
    private String zstate;

    @Column(name = "zone")
    private String zone;

    @Column(name = "tier")
    private String tier;

    @Column(name = "state")
    private String state;

    @Column(name = "active")
    private Boolean active;
}
