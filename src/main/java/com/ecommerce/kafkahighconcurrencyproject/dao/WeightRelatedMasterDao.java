package com.ecommerce.kafkahighconcurrencyproject.dao;

import co.fareye.model.WeightRelated;

import java.io.Serializable;

public interface WeightRelatedMasterDao extends BaseDao<WeightRelated, Serializable> {

    WeightRelated findByClientId(String clientId);


}

