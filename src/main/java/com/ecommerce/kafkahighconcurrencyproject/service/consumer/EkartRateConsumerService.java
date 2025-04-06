/**
 * ****************************************************************************
 * <p>
 * Copyright (c) 2023, FarEye and/or its affiliates. All rights
 * reserved.
 * ___________________________________________________________________________________
 * <p>
 * <p>
 * NOTICE: All information contained herein is, and remains the property of
 * FarEye and its suppliers,if any. The intellectual and technical concepts
 * contained herein are proprietary to FarEye. and its suppliers and
 * may be covered by us and Foreign Patents, patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from FarEye.
 */
package com.ecommerce.kafkahighconcurrencyproject.service.consumer;

import co.fareye.config.ConfigProperty;
import co.fareye.constant.AppConstant;
import co.fareye.dao.DiscountMasterDao;
import co.fareye.dao.EkartMasterDao;
import co.fareye.dao.StateClientTaxMasterDao;
import co.fareye.dto.EkartRateDto;
import co.fareye.dto.ProcessData;
import co.fareye.model.DiscountMaster;
import co.fareye.model.EkartMaster;
import co.fareye.model.StateClientMaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static co.fareye.service.consumer.EkartRawDataConsumer.get20thDateOfMonth;
import static co.fareye.service.consumer.EkartRawDataConsumer.getDateTimeInZone;

/**
 *
 * @author Ashish Thalia
 * @since 14-Feb-2023, 07:28:00 PM
 */

@Service
@Log4j2
public class EkartRateConsumerService extends BaseConsumerService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EkartMasterDao ekartMasterDao;

    @Autowired
    DiscountMasterDao discountMasterDao;

    @Autowired
    StateClientTaxMasterDao stateClientTaxMasterDao;




    EkartRateConsumerService(final KafkaListenerContainerFactory kafkaListenerContainerFactory, final ConfigProperty configProperty) {
        super(kafkaListenerContainerFactory, "RetryWebhookConsumerService", configProperty.getEkartRateTopic(), Integer.parseInt(configProperty.getEkartConsumerConcurrency()), configProperty.getEkartConsumerStatus());
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        int concurrency = Integer.parseInt(configProperty.getEkartConsumerConcurrency());
        String topic = configProperty.getEkartRateTopic();
        String status = configProperty.getEkartConsumerStatus();
        Set<String> topics = new HashSet<>();
        topics.add(topic);
        // Call refreshContainers method for making changes to container properties
        refreshContainers(concurrency, topics, status);
    }

    @Override
    public boolean consumeMessage(List<ConsumerRecord<String, Object>> consumerRecords) {
        // Step 1 - Convert list of consumer records into list of dto objects
        try {
            log.error(consumerRecords.size());
            //List<EkartRateDto> ekartRateDtos = Arrays.asList(objectMapper.readValue(objectMapper.writeValueAsString(consumerRecords.stream().map(ConsumerRecord::value).collect(Collectors.toList())), EkartRateDto[].class));
            // Step 2 - Pause consumer so that it can't consume another new message from same partition until it completes process for already consumed message
            kafkaListenerUtil.pauseConsumer(configProperty.getEkartRateTopic());
            CompletableFuture.runAsync(() -> pushToAsync(consumerRecords)).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.info("Resuming consumer");
                    kafkaListenerUtil.resumeConsumer(configProperty.getEkartRateTopic());
                    log.error(exception);
                }
            }).thenRun(() -> {
                log.info("Resuming consumer");
                kafkaListenerUtil.resumeConsumer(configProperty.getEkartRateTopic());
            });
            return true;
        } catch (Exception e) {
            kafkaListenerUtil.resumeConsumer(configProperty.getEkartRateTopic());
            log.error(e.getMessage());
            return false;
        }
    }
    private void pushToAsync(List<ConsumerRecord<String, Object>> consumerRecords) {
        try {
            Map<String,EkartRateDto> rateMap = new HashMap<>();
            Map<String,EkartMaster> dataMap;
            dataMap = findData(consumerRecords);
            consumerRecords.forEach(consumerRecord -> {
                try {
                    EkartRateDto ekartRateDto = objectMapper.convertValue(consumerRecord.value(),EkartRateDto.class);
                    rateMap.put(ekartRateDto.getRateCalculationProcessRequestDTO().getReferenceNumber(),ekartRateDto);
                    if(Objects.equals(ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode(), "No Rate Required")){
                        ProcessData processData = objectMapper.convertValue(ekartRateDto.getRateCalculationProcessRequestDTO().getProcessMasterCode(), ProcessData.class);
                        EkartMaster ekartMaster = saveData(false,processData,dataMap);
                        if(ekartMaster!=null) {
                            dataMap.put(ekartMaster.getTrackingId(), ekartMaster);
                        }
                    }else {
                        long startTime = System.currentTimeMillis();

                        //DB Call

                        List<DiscountMaster> discounts = discountMasterDao.findAll();
                        Map<String, DiscountMaster> discountMap = new HashMap<>();
                        for (DiscountMaster discount : discounts) {
                            discountMap.put(discount.getClientId(), discount);
                        }



                        EkartMaster ekartMaster = taxCalculate(ekartRateDto, dataMap,discountMap);
                        if(ekartMaster!=null) {
                            dataMap.put(ekartMaster.getTrackingId(), ekartMaster);
                        }
                        long endTime = System.currentTimeMillis();
                        long difference = endTime - startTime;
                        log.error("Time for tax calculate: "+ difference);
                    }
                } catch (Exception e) {
                    log.error("LOGIC-Error in tax calculation: "+e);
                    e.printStackTrace();
                }
            });
            saveAllData(dataMap,rateMap);
        } catch (Exception e) {
            log.error("LOGIC-Error in overall tax calculation: "+e);
            e.printStackTrace();
        }
    }

    private EkartMaster taxCalculate(EkartRateDto ekartRateDto, Map<String,EkartMaster> dataMap,Map<String,DiscountMaster> discountMap){
        EkartMaster ekartMaster = null;
        boolean isDataSaved = false;
        ProcessData processData = null;
        try {
            if(dataMap.containsKey(ekartRateDto.getRateCalculationProcessRequestDTO().getReferenceNumber())&& dataMap.get(ekartRateDto.getRateCalculationProcessRequestDTO().getReferenceNumber()).getIsProcessed()){
                processData = objectMapper.readValue(dataMap.get(ekartRateDto.getRateCalculationProcessRequestDTO().getReferenceNumber()).getData(),ProcessData.class);
            }else {
                processData = objectMapper.convertValue(ekartRateDto.getRateCalculationProcessRequestDTO().getProcessMasterCode(), ProcessData.class);
            }
            String additionalchar = "F";
            String timeZone = "Asia/Kolkata"; // IST time zone
            LocalDateTime presentDate = getDateTimeInZone(timeZone);
            DecimalFormat df = new DecimalFormat("#.##");
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(2);
            processData.setFreightCharge(0);

            if (ekartRateDto.getErrorMessage() == null || ekartRateDto.getErrorMessage().isEmpty()) {
                if (ekartRateDto.getBreakUp() != null && ekartRateDto.getBreakUp().getAdditionalCharges() != null) {
                    additionalchar = "T";
                }

                processData.setFreightCharge(ekartRateDto.getBreakUp().getFreightCharge());

                if (additionalchar.equals("T")) {
                    List<EkartRateDto.BreakUp.AdditionalCharges> add_charge = ekartRateDto.getBreakUp().getAdditionalCharges();

                    for (EkartRateDto.BreakUp.AdditionalCharges charge : add_charge) {
                        if ("Fuel Surcharge Exp".equals(charge.getChargeHead())) {
                            processData.setFuelSurchargeExp(charge.getAmount());
                        }

                        if ("Fuel Surcharge Std".equals(charge.getChargeHead())) {
                            processData.setFuelSurchargeStd(charge.getAmount());
                        }

                        if ("Fuel Surcharge".equals(charge.getChargeHead())) {
                            processData.setFuelSurchargeExceptional(charge.getAmount());
                        }

                        if (processData.getPaymentMode().equals("COD")) {
                            if ("COD charges".equals(charge.getChargeHead())) {
                                processData.setCodCharges(charge.getAmount());

                                if (processData.getShipmentType().equals("RTO") && ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode().equals("NEGATIVE_COD")) {
                                    processData.setCodCharges(-processData.getCodCharges());
                                }
                            }

                            if ("POS Charge".equals(charge.getChargeHead())) {
                                processData.setPosCharge(charge.getAmount());
                            }
                        }
                    }

                    if (processData.getFuelSurchargeExp() == 0) {
                        processData.setFuelSurchargeExp(0);
                    }

                    if (processData.getFuelSurchargeStd() == 0) {
                        processData.setFuelSurchargeStd(0);
                    }

                    if (processData.getCodCharges() == 0) {
                        processData.setCodCharges(0);
                    }

                    if (processData.getPosCharge() == 0) {
                        processData.setPosCharge(0);
                    }
                    if(Objects.equals(processData.getClientId(), "XMI")){
                        processData.setCodCharges(Math.min(100.0,processData.getCodCharges()));

                        if(processData.getCodCharges()<-100){
                            processData.setCodCharges(-100);
                        }
                    }
                }

                processData.setIsFreightFound("T");
            } else if (!"no freight charges found for the request".equals(ekartRateDto.getErrorMessage())) {
                processData.setIsDiffError("T");
            }

            if (Objects.equals(processData.getIsFreightFound(), "T")) {
                processData.setCodRevenue(processData.getCodCharges());
                processData.setPosRevenue(processData.getPosCharge());

                if (processData.getShipmentType().equals("FORWARD") && ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode().isEmpty()) {
                    processData.setFwdShippingRevenue(Double.parseDouble(df.format(processData.getFreightCharge())));
                    if (processData.getShipmentStatus().equals("delivered") && presentDate.isAfter(LocalDateTime.parse(get20thDateOfMonth(processData.getFwdMonth() + 1, presentDate.getYear())))) {
                        processData.setFwdShippingRevenue(0);
                    }
                }

                if (processData.getShipmentType().equals("RTO") && ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode().equals("RTO_FORWARD")) {
                    processData.setFwdShippingRevenue(Double.parseDouble(df.format(processData.getFreightCharge())));
                    // throw processData.getFreightCharge();
                }
                if (processData.getShipmentType().equals("FORWARD") && ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode().equals("RTO_FORWARD")) {
                    processData.setFwdShippingRevenue(Double.parseDouble(df.format(processData.getFreightCharge())));
                    processData.setShipmentType("RTO");
                    // throw processData.getFreightCharge();
                }

                if (processData.getShipmentType().equals("RTO") && ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode().isEmpty()) {
                    processData.setRtoRevenue(Double.parseDouble(df.format(processData.getFreightCharge())));
                    processData.setPosRevenue(0);
                    processData.setCodRevenue(0);
                }

                if (processData.getShipmentType().equals("RTO") && ekartRateDto.getRateCalculationProcessRequestDTO().getFlowCode().equals("NEGATIVE_COD")) {
                    processData.setRtoRevenue(Double.parseDouble(df.format(processData.getFreightCharge())));
                    processData.setFwdShippingRevenue(0);
                }

                if (processData.getShipmentType().equals("RVP")) {
                    processData.setRvpRevenue(Double.parseDouble(df.format(processData.getFreightCharge())));
                    processData.setCodRevenue(0);
                    processData.setPosRevenue(0);
                }

                if (processData.getPaymentMode().equals("PREPAID") || processData.getPaymentMode().equals("PP")) {
                    processData.setCodRevenue(0);
                    processData.setPosRevenue(0);
                }


                processData.setCodPosRevenue(Double.parseDouble(df.format(processData.getPosRevenue() + processData.getCodRevenue())));

                double x = processData.getPosRevenue();
                processData.setPosRevenue(Double.parseDouble(df.format(x)));


                processData.setTotalShippingRevenue(Double.parseDouble(df.format(processData.getFwdShippingRevenue() +
                        processData.getRtoRevenue() + processData.getRvpRevenue())));

                double totalShippingRevenues=processData.getTotalShippingRevenue();

                if(processData.getShipmentType().equals("RVP")) {
                    double rvpSurcharges=processData.getRvpSurcharges();
                    totalShippingRevenues-=rvpSurcharges;
                }

                if (processData.getExpOrStd().equals("EXP")) {
                    processData.setFuelSurcharges(processData.getFuelSurchargeExp() *
                            totalShippingRevenues / 100);
                    processData.setFuelSurcharges(Double.parseDouble(df.format(processData.getFuelSurcharges())));
                } else {
                    processData.setFuelSurcharges(processData.getFuelSurchargeStd() *
                            totalShippingRevenues / 100);
                    processData.setFuelSurcharges(Double.parseDouble(df.format(processData.getFuelSurcharges())));
                }

                if(Objects.equals(processData.getClientId(), "XMI") && (Objects.equals(processData.getZone(), "LOCAL") || Objects.equals(processData.getZone(), "ZONAL"))){
                    processData.setFuelSurcharges(Double.parseDouble(df.format(processData.getFuelSurchargeExceptional()*totalShippingRevenues/ 100)));
                }

                //Discount Logic

                processData.setGrossShipping(processData.getFuelSurcharges()+processData.getTotalShippingRevenue());

                processData.setGrossRevenue(processData.getGrossShipping()+processData.getCodPosRevenue());

               if(discountMap.containsKey(processData.getClientId()))
               {
                   DiscountMaster discountMaster=discountMap.get(processData.getClientId());
                   if(discountMaster.getActivationStatus()){

                    if(discountMaster.getAppliedOn().equals("FLAT")){
                        processData.setShippingDiscount(discountMaster.getFreightDiscountPercentage());
                        processData.setCodDiscount(0);
                        if((processData.getClientId().equalsIgnoreCase("OIP") || processData.getClientId().equalsIgnoreCase("GLA")||processData.getClientId().equalsIgnoreCase("FMB"))
                                && processData.getShipmentType().equalsIgnoreCase("RVP")){
                            processData.setShippingDiscount(0);
                        }

//                        if(processData.getClientId().equalsIgnoreCase("SAB") || processData.getClientId().equalsIgnoreCase("SIR")
//                                || processData.getClientId().equalsIgnoreCase("SYS") || processData.getClientId().equalsIgnoreCase("AMI") ){
//
//                        }
                    }
                    else{
                   processData.setShippingDiscount(Double.parseDouble(df.format((processData.getGrossShipping()*discountMaster.getFreightDiscountPercentage())/100)));

                        // Exceptional for SDL-RVP Shipments
                        if(processData.getClientId().equalsIgnoreCase("SDL") && processData.getShipmentType().equalsIgnoreCase("RVP")){
                            processData.setShippingDiscount(Double.parseDouble(df.format((processData.getGrossShipping()*10)/100)));
                        }

                       if(discountMaster.getAppliedOn().equals("FREIGHT"))
                       {
                           processData.setCodDiscount(0);
                       }
                       else
                       {
                           processData.setCodDiscount(Double.parseDouble(df.format((processData.getCodPosRevenue()*discountMaster.getCodDiscountPercentage())/100)));
                       }
                    }

                         if(processData.getGrossRevenue()==0.0)
                         {
                             processData.setCodDiscount(0);
                             processData.setShippingDiscount(0);
                         }
                   processData.setTotalDiscount(Double.parseDouble(df.format(processData.getShippingDiscount()+processData.getCodDiscount())));

               }

               }

                processData.setNetShipping(Double.parseDouble(df.format(processData.getGrossShipping()-processData.getShippingDiscount())));
                processData.setNetCod(Double.parseDouble(df.format(processData.getCodPosRevenue()-processData.getCodDiscount())));
                processData.setNetRevenue(Double.parseDouble(df.format(processData.getNetShipping()+processData.getNetCod())));

               //Discount Logic Ends here

                processData.setTotalAmountWithoutTax(Double.parseDouble(df.format(processData.getFwdShippingRevenue() +
                        processData.getRtoRevenue() + processData.getRvpRevenue() +
                        processData.getFuelSurcharges() + processData.getCodRevenue() +
                        processData.getPosRevenue())));

                processData.setTotalRevenue(Double.parseDouble(df.format(processData.getTotalAmountWithoutTax())));


                Map<String, StateClientMaster> stateMap = new HashMap<>();
                stateMap = getClientStateMap();
                String sourceStateLocal=processData.getBillFromState();

                if(stateMap.containsKey(processData.getClientId()) && stateMap.get(processData.getClientId()).getState().contains(sourceStateLocal)){
                    processData.setCgstTaxRate("9%");
                    processData.setSgstTaxRate("9%");
                    processData.setIgstTaxRate("0%");
                    processData.setCgstTotalAmount((processData.getNetRevenue() / 100) * 9);
                    processData.setSgstTotalAmount((processData.getNetRevenue() / 100) * 9);
                    processData.setIgstTotalAmount(0);
                } else {
                    processData.setCgstTaxRate("0%");
                    processData.setSgstTaxRate("0%");
                    processData.setIgstTaxRate("18%");
                    processData.setCgstTotalAmount(0);
                    processData.setSgstTotalAmount(0);
                    processData.setIgstTotalAmount((processData.getNetRevenue() / 100) * 18);
                }



                double x1 = processData.getCgstTotalAmount();
                processData.setCgstTotalAmount(Double.parseDouble(df.format(x1)));

                double x2 = processData.getSgstTotalAmount();
                processData.setSgstTotalAmount(Double.parseDouble(df.format(x2)));

                double x3 = processData.getIgstTotalAmount();
                processData.setIgstTotalAmount(Double.parseDouble(df.format(x3)));

                processData.setTotalTax(processData.getCgstTotalAmount() +
                        processData.getSgstTotalAmount() + processData.getIgstTotalAmount());

                double x4 = processData.getTotalTax();
                processData.setTotalTax(Double.parseDouble(df.format(x4)));

                processData.setTotalRevenueTax(Double.parseDouble(df.format(processData.getNetRevenue() +
                        processData.getTotalTax())));

                double x5 = processData.getTotalRevenueTax();
                processData.setTotalRevenueTax(Double.parseDouble(df.format(x5)));

                processData.setTotalAmountWithTax(Double.parseDouble(df.format(processData.getTotalAmountWithoutTax() +
                        processData.getCgstTotalAmount() + processData.getSgstTotalAmount() +
                        processData.getIgstTotalAmount())));

                processData.setValueIncludingTax(Double.parseDouble(df.format(Double.parseDouble(processData.getValue()) + processData.getTotalTax())));

                double x6 = processData.getTotalAmountWithTax();
                processData.setTotalAmountWithTax(Double.parseDouble(df.format(x6)));

                processData.setSacCode(93);
                processData.setFeeName(93);
                processData.setErrorMessage("No Error");
            } else {
                processData.setFwdShippingRevenue(0);
                processData.setRtoRevenue(0);
                processData.setRvpRevenue(0);
                processData.setFuelSurcharges(0);
                processData.setCodRevenue(0);
                processData.setPosRevenue(0);
                processData.setTotalAmountWithoutTax(0);
                processData.setTotalRevenueTax(0);
                processData.setTotalTax(0);
                processData.setCgstTotalAmount(0);
                processData.setSgstTotalAmount(0);
                processData.setIgstTotalAmount(0);
                processData.setSacCode(0);
                processData.setFeeName(0);

                if ("T".equals(processData.getIsDiffError())) {
                    processData.setErrorMessage(ekartRateDto.getErrorMessage());
                } else {
                    processData.setErrorMessage("No Freight charge found for this request");
                }
            }
            boolean updateStatus;
            updateStatus = Objects.equals(processData.getErrorMessage(), "No Error");
            isDataSaved = true;
            ekartMaster = saveData(updateStatus, processData,dataMap);
        }catch(Exception ex){
            log.error("LOGIC-Error in tax logic: "+ex);
            ex.printStackTrace();
        }
        if(!isDataSaved){
            ekartMaster = saveData(false,processData,dataMap);
        }
        return ekartMaster;
    }

    private EkartMaster saveData(boolean updateStatus, ProcessData processData,Map<String,EkartMaster> dataMap){
        long startTime = System.currentTimeMillis();
        EkartMaster newEkartMaster = new EkartMaster();
        try {
            if(dataMap.containsKey(processData.getTrackingId())){
                newEkartMaster = dataMap.get(processData.getTrackingId());
            }
            String data = objectMapper.writeValueAsString(processData);
            newEkartMaster.setTrackingId(processData.getTrackingId());
            newEkartMaster.setClientId(processData.getClientId());
            newEkartMaster.setIsUpdated(updateStatus);
            newEkartMaster.setMerchantId(processData.getMerchantId());
            newEkartMaster.setData(data);
            newEkartMaster.setFwdMonth(processData.getFwdMonth());
            newEkartMaster.setRtoMonth(processData.getRtoMonth());
            newEkartMaster.setRvpMonth(processData.getRvpMonth());
            newEkartMaster.setPosMonth(processData.getPosMonth());
            newEkartMaster.setDirectDeliveredMonth(processData.getDirectDeliveredMonth());
            newEkartMaster.setIntFileStatus(AppConstant.UN_UPLOADED);
            newEkartMaster.setIsProcessed(true);
            newEkartMaster.setRateUpdated(true);
            log.error("Tracking Id pushed to array : "+processData.getTrackingId());
        }catch (Exception exception){
            log.error("DB-Error Save data payload error:{}",exception.getMessage());
            exception.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        long difference = endTime-startTime;
//        log.error("Time to save: " + difference);
        return newEkartMaster;
    }

    private Map<String,EkartMaster> findData(List<ConsumerRecord<String, Object>> consumerRecords){
        List<String> trackingList = new ArrayList<>();
        Map<String,EkartMaster> dataMap = new HashMap<>();
        consumerRecords.forEach(consumerRecord -> {
            try{
                EkartRateDto ekartRateDto = objectMapper.convertValue(consumerRecord.value(),EkartRateDto.class);
                trackingList.add(ekartRateDto.getRateCalculationProcessRequestDTO().getReferenceNumber());

            }catch(Exception e){
                log.error("DB-Error in finding Data: "+e.getMessage());
            }
        });
        List<EkartMaster> dataList = ekartMasterDao.findByTrackingIdIn(trackingList);
        if(!dataList.isEmpty()) {
            dataList.forEach(ekartMaster -> {
                dataMap.put(ekartMaster.getTrackingId(), ekartMaster);
            });
        }
        return dataMap;
    }

    private void saveAllData(Map<String,EkartMaster> dataMap, Map<String,EkartRateDto> rateMap){
        try {
            long startSaveTime = System.currentTimeMillis();
            List<EkartMaster> listOfEkartMasters = new ArrayList<>(dataMap.values());
            ekartMasterDao.saveAll(listOfEkartMasters);
            long endSaveTime = System.currentTimeMillis();
            long saveDifference = endSaveTime - startSaveTime;
            log.error("Time for batch Save: "+ saveDifference);
        }catch(Exception e) {
            log.error("DB-Error in batch Save: " + e.getMessage());
            List<EkartMaster> listOfMasters = new ArrayList<>(dataMap.values());
            listOfMasters.forEach(ekartMaster -> {
                try {
                    long startTime = System.currentTimeMillis();
                    ekartMasterDao.save(ekartMaster);
                    long endTime = System.currentTimeMillis();
                    long difference = endTime - startTime;
                    log.error("Time for single insertion: " + difference);
                }catch (Exception ex){
                    findAndInsertFailed(ekartMaster,rateMap);
                }
            });
        }
    }

    private void findAndInsertFailed(EkartMaster ekartFailedMaster, Map<String,EkartRateDto> rateMap){
        Map<String,EkartMaster> failedDataMap = new HashMap<>();
        try {

            //DB Call

            List<DiscountMaster> discounts = discountMasterDao.findAll();
            Map<String, DiscountMaster> discountMap = new HashMap<>();
            for (DiscountMaster discount : discounts) {
                discountMap.put(discount.getClientId(), discount);
            }

            EkartMaster ekartFound = ekartMasterDao.findByTrackingId(ekartFailedMaster.getTrackingId());
            failedDataMap.put(ekartFailedMaster.getTrackingId(),ekartFound);
            EkartMaster newEkartMaster = taxCalculate(rateMap.get(ekartFailedMaster.getTrackingId()),failedDataMap,discountMap);
            newEkartMaster.setId(ekartFound.getId());
            ekartMasterDao.save(newEkartMaster);
            log.error("Saved Failed tracking Id: "+ekartFailedMaster.getTrackingId());
        }catch (Exception e){
            log.error("Reinsertion Failed: "+e);
        }
    }

    private Map<String, StateClientMaster> getClientStateMap() {

        Map<String, StateClientMaster> stateMap = new HashMap<>();
        try {
        List<StateClientMaster> stateClientList = stateClientTaxMasterDao.findAll();
        for (StateClientMaster sateClient : stateClientList) {
            stateMap.put(sateClient.getClientCode(), sateClient);
        }
            return stateMap;
        }
        catch (Exception e) {
            log.error("Error in merchant map:"+e.getMessage());
            e.printStackTrace();
        }
        return stateMap;
    }
}
