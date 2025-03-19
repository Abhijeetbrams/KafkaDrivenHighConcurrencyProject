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
package com.ecommerce.kafkahighconcurrencyproject.dao;

import co.fareye.model.PincodeMaster;

import java.io.Serializable;

/**
 * This repository is used to handle all DB level operations with AppVariable Entity
 *
 * @author Shubham Panth
 */
public interface PincodeMasterDao extends BaseDao<PincodeMaster, Serializable> {

    PincodeMaster findByPincode(String pincode);

}
