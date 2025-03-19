
package com.ecommerce.kafkahighconcurrencyproject.dao;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface EkartMasterDao extends BaseDao<EkartMaster, Serializable> {

    EkartMaster findByTrackingId(String trackingId);

    List<EkartMaster> findByTrackingIdIn(List<String> trackingIds);

    @Query(value="SELECT e.tracking_id FROM Ekart e WHERE e.is_processed = true AND e.fwd_month IS NULL AND e.rto_month = :rtoMonth AND CAST(e.data AS jsonb)->>'rto_revenue' = '0.0' AND e.is_updated = true",nativeQuery = true)
    List<String> findTrackingIdsByRtoMonth(@Param("rtoMonth") int rtoMonth);

    @Query(value="SELECT e.tracking_id FROM Ekart e WHERE e.is_processed = true AND e.fwd_month IS NULL AND e.rto_month = :rtoMonth AND CAST(e.data AS jsonb)->>'fwd_shipping_revenue' = '0.0' AND e.is_updated = true",nativeQuery = true)
    List<String> findTrackingIdsByFWDRtoMonth(@Param("rtoMonth") int rtoMonth);

    @Query(value="SELECT e.tracking_id FROM Ekart e WHERE e.is_processed = true AND e.fwd_month IS NULL AND e.rto_month = :rtoMonth AND CAST(e.data AS jsonb)->>'error_message' = 'No Freight charge found for this request'",nativeQuery = true)
    List<String> findTrackingIdsByRtoMonthAndErrorMessage(@Param("rtoMonth") int rtoMonth);

    @Query(value="SELECT e.tracking_id FROM Ekart e WHERE e.is_processed = true AND (CAST(e.data AS jsonb)->>'error_message' = 'Source Pincode is blank or not present in the master' OR CAST(e.data AS jsonb)->>'error_message' = 'Destination Pincode is blank or not present in the master')",nativeQuery = true)
    List<String> findTrackingIdsByPincodeErrorMessages();

    @Query(value="SELECT COUNT(e), e.client_id as clientId FROM Ekart e " +
            "WHERE e.is_processed = true AND " +
            "CAST(e.data AS jsonb)->>'shipment_type' = 'FORWARD' AND " +
            "CAST(e.data AS jsonb)->>'fwd_shipping_revenue' = '0.0' AND " +
            "CAST(e.data AS jsonb)->>'error_message' = 'No Error' AND " +
            "CAST(e.data AS jsonb)->>'m1_m2_flag' = 'M1' " +
            "GROUP BY e.client_id",nativeQuery = true)
    List<ClientCountDTO> countByClientIdAndFWDCriteria();

    @Query(value="SELECT COUNT(e) as count, e.client_id as clientId FROM Ekart e " +
            "WHERE e.is_processed = true AND " +
            "CAST(e.data AS jsonb)->>'shipment_type' = 'RTO' AND " +
            "CAST(e.data AS jsonb)->>'rto_revenue' = '0.0' AND " +
            "CAST(e.data AS jsonb)->>'error_message' = 'No Error' AND " +
            "CAST(e.data AS jsonb)->>'m1_m2_flag' = 'M1' " +
            "GROUP BY e.client_id",nativeQuery = true)
    List<ClientCountDTO> countByClientIdAndRTOCriteria();

    @Query(value="SELECT COUNT(e) as count, e.client_id as clientId FROM Ekart e " +
            "WHERE e.is_processed = true AND " +
            "CAST(e.data AS jsonb)->>'shipment_type' = 'RVP' AND " +
            "CAST(e.data AS jsonb)->>'rvp_revenue' = '0.0' AND " +
            "CAST(e.data AS jsonb)->>'error_message' = 'No Error' AND " +
            "CAST(e.data AS jsonb)->>'m1_m2_flag' = 'M1' " +
            "GROUP BY e.client_id",nativeQuery = true)
    List<ClientCountDTO> countByClientIdAndRVPCriteria();

    @Query(value="SELECT COUNT(e) as count, e.client_id as clientId FROM Ekart e " +
            "WHERE e.is_processed = true AND " +
            "CAST(e.data AS jsonb)->>'error_message' = 'Client ID is not present in Merchant database' " +
            "GROUP BY e.client_id",nativeQuery = true)
    List<ClientCountDTO> countByClientIdForErrorMessage();

    @Query(value="SELECT COUNT(e) as count, CAST(e.data AS jsonb)->>'error_message' as errorMessage " +
            "FROM Ekart e " +
            "WHERE e.is_processed = true " +
            "GROUP BY CAST(e.data AS jsonb)->>'error_message'",nativeQuery = true)
    List<ErrorMessageCountDTO> countByDistinctErrorMessage();

    @Query(name="SELECT COUNT(e) FROM Ekart e WHERE e.is_processed = true",nativeQuery = true)
    long countByIsProcessedTrue();
//    Page<EkartMaster> findByIsUpdatedAndIsProcessedTrueAndClientIdAndIntFileStatus(boolean isUpdated, String clientId, String fileStatus, Pageable pageable);

    @Query(value = "UPDATE ekart SET int_file_status ='UPLOADED' WHERE id in (SELECT id  from ekart where is_updated= :isUpdated and is_processed=TRUE and client_id= :clientId and  int_file_status='UNUPLOADED'  limit 1500) RETURNING *", nativeQuery = true)
    List<Map<String,Object>> findByIsUpdatedAndIsProcessedTrueAndClientIdAndIntFileStatus(boolean isUpdated,String clientId);


    @Modifying
    @Transactional
    @Query(value = "UPDATE ekart SET is_processed = false WHERE is_processed = true",nativeQuery = true)
    int updateIsProcessed();

    @Modifying
    @Transactional
    @Query(value="UPDATE ekart SET int_file_status = 'UNUPLOADED' WHERE int_file_status = 'UPLOADED' AND is_processed = true",nativeQuery = true)
    int updateIntFileStatus();

    @Modifying
    @Transactional
        @Query(value = "UPDATE ekart SET is_processed = false WHERE is_processed = true AND client_id IN (:clientIds)",nativeQuery = true)
    int updateIsProcessedByClientIds(List<String> clientIds);
}