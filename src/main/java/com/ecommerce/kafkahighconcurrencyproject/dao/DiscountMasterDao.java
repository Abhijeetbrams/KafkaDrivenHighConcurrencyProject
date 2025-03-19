package com.ecommerce.kafkahighconcurrencyproject.dao;

import co.fareye.model.DiscountMaster;

import java.io.Serializable;

public interface DiscountMasterDao extends BaseDao<DiscountMaster, Serializable> {

    DiscountMaster findByClientId(String clientId);


}
