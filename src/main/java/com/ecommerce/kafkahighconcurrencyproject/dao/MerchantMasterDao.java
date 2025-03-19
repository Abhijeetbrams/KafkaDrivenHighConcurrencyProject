package com.ecommerce.kafkahighconcurrencyproject.dao;

import co.fareye.model.MerchantMaster;

import java.io.Serializable;

public interface MerchantMasterDao extends BaseDao<MerchantMaster, Serializable> {

    MerchantMaster findByClientId(String clientId);
}

