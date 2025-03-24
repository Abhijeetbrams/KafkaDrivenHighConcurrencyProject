/**
 * ****************************************************************************
 *
 * Copyright (c) 2023, FarEye and/or its affiliates. All rights
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

import co.fareye.model.AbstractEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 *
 * @author Shubham Panth
 */
@Data
@Entity
@Table(name = "`ekart`", catalog = "ekart-db")
public class EkartMaster extends AbstractEntity {


    @Column(name = "tracking_id")
    private String trackingId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "is_processed")
    private Boolean isProcessed;

    private String data;

    @Column(name = "is_updated")
    private Boolean isUpdated;

    @Column(name = "int_file_upload_time")
    private LocalDateTime intFileUploadTime;

    @Column(name = "int_file_last_retry_time")
    private LocalDateTime intFileLastRetryTime;

    @Column(name = "int_file_failed_reason")
    private String intFileFailedReason;

    @Column(name = "int_file_invocation_id")
    private String intFileInvocationId;

    @Column(name = "int_file_status")
    private String intFileStatus = "UNUPLOADED";

    @Column(name = "int_file_filename")
    private String intFileFilename;

    @Column(name = "int_file_retry_count")
    private Integer intFileRetryCount;

    @Column(name = "fwd_month")
    private Integer fwdMonth;

    @Column(name = "pos_month")
    private Integer posMonth;

    @Column(name = "rto_month")
    private Integer rtoMonth;

    @Column(name = "rvp_month")
    private Integer rvpMonth;

    @Column(name = "direct_delivered_month")
    private Integer directDeliveredMonth;

    @Column(name = "rate_updated")
    private Boolean rateUpdated;
}
