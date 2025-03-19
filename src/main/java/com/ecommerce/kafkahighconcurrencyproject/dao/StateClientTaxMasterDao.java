package com.ecommerce.kafkahighconcurrencyproject.dao;

import co.fareye.model.StateClientMaster;

import java.io.Serializable;

public interface StateClientTaxMasterDao extends BaseDao<StateClientMaster, Serializable> {
    StateClientMaster findByClientCode(String clientCode);

}
