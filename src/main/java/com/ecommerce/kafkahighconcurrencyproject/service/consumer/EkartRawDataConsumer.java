
package com.ecommerce.kafkahighconcurrencyproject.service.consumer;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import com.ecommerce.kafkahighconcurrencyproject.dao.EkartMasterDao;
import com.ecommerce.kafkahighconcurrencyproject.dao.MerchantMasterDao;
import com.ecommerce.kafkahighconcurrencyproject.dao.PincodeMasterDao;
import com.ecommerce.kafkahighconcurrencyproject.dao.WeightRelatedMasterDao;
import com.ecommerce.kafkahighconcurrencyproject.dto.EkartRateDto;
import com.ecommerce.kafkahighconcurrencyproject.dto.ProcessData;
import com.ecommerce.kafkahighconcurrencyproject.dto.RateCalculationRequestDTO;
import com.ecommerce.kafkahighconcurrencyproject.dto.StateMasterDto;
import com.ecommerce.kafkahighconcurrencyproject.model.EkartMaster;
import com.ecommerce.kafkahighconcurrencyproject.model.MerchantMaster;
import com.ecommerce.kafkahighconcurrencyproject.model.PincodeMaster;
import com.ecommerce.kafkahighconcurrencyproject.model.WeightRelated;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
@Log4j2
public class EkartRawDataConsumer extends BaseConsumerService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EkartMasterDao ekartMasterDao;

    @Autowired
    PincodeMasterDao pincodeMasterDao;

    @Autowired
    MerchantMasterDao merchantMasterDao;

    @Autowired
    WeightRelatedMasterDao weightRelatedMasterDao;


    @Autowired
    @Qualifier("StringKeyValueProducer")
    KafkaProducer kafkaProducer;


    EkartRawDataConsumer(final KafkaListenerContainerFactory kafkaListenerContainerFactorySecondary, final ConfigProperty configProperty) {
        super(kafkaListenerContainerFactorySecondary, "EkartRawDataConsumerService", configProperty.getEkartRawDataTopic(), Integer.parseInt(configProperty.getEkartRawDataConsumerConcurrency()), configProperty.getEkartRawDataConsumerStatus());
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        int concurrency = Integer.parseInt(configProperty.getEkartRawDataConsumerConcurrency());
        String topic = configProperty.getEkartRawDataTopic();
        String status = configProperty.getEkartRawDataConsumerStatus();
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
            log.info("Consumer Records" + consumerRecords.toString());
            RawDataJson rawDataJson = objectMapper.readValue(objectMapper.writeValueAsString(consumerRecords.stream().map(ConsumerRecord::value).collect(Collectors.toList()).get(0)), RawDataJson.class);
            List<DownloadRawData> downloadRawData = rawDataJson.getRawDataList();
            log.info("Ekart Rate" + downloadRawData.toString());
            // Step 2 - Pause consumer so that it can't consume another new message from same partition until it completes process for already consumed message
            kafkaListenerUtil.pauseConsumer(configProperty.getEkartRawDataTopic());
            CompletableFuture.runAsync(() -> processRate(downloadRawData)).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.info("Resuming consumer");
                    kafkaListenerUtil.resumeConsumer(configProperty.getEkartRawDataTopic());
                    log.error(exception);
                }
            }).thenRun(() -> {
                log.info("Resuming consumer");
                kafkaListenerUtil.resumeConsumer(configProperty.getEkartRawDataTopic());
            });
            return true;
        } catch (Exception e) {
            kafkaListenerUtil.resumeConsumer(configProperty.getEkartRawDataTopic());
            log.error("Consumer Error : "+e.getMessage());
            return false;
        }
    }
    private void processRate(List<DownloadRawData> downloadRawDataList) {
        try {
//            List<CompletableFuture<Void>> futList = new ArrayList<>();
//            for(DownloadRawData downloadRawData: downloadRawDataList){
//                CompletableFuture<Void> ruleFuture = CompletableFuture.runAsync(
//                        ()->rateLogic(downloadRawData)
//                );
//                futList.add(ruleFuture);
//            }
//
//            CompletableFuture.allOf(futList.toArray(new CompletableFuture[0])).join();

            downloadRawDataList.forEach(downloadRawData -> {
                try {
                    rateLogic(downloadRawData);
                } catch (Exception e) {
                    log.error(e);
                }
            });

        } catch (Exception e) {
            log.error("Error in Process Rate : "+e);
            e.printStackTrace();
        }
    }

    private void rateLogic(DownloadRawData downloadRawData){
        boolean isSaved = false;
        ProcessData processData = null;
        try {
            String pattern = "yyyy-MM-dd HH:mm:ss";
            String timeZone = "Asia/Kolkata"; // IST time zone
            LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of(timeZone));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

            LocalDateTime presentDate = getDateTimeInZone(timeZone);

            Map<String, MerchantMaster> merchantMap = new HashMap<>();
            Map<String, StateMasterDto> stateMap = new HashMap<>();
            Map<String, WeightRelated> weightRelatedMap = new HashMap<>();

            processData = downloadRawData.getProcessData();
            merchantMap = getMerchantMap();
            stateMap = getstateMap();
            weightRelatedMap= getWeightRelated();

            processData.setBookingDateTime(localDateTime.format((formatter)));
            processData.setValue(processData.getShipmentValue());
            processData.setFlag("RI");

            EkartMaster ekartMaster = getDataByTrackingId(downloadRawData.getProcessData().getTrackingId());
            if (ekartMaster != null) {
                processData.setFwdMonth(ekartMaster.getFwdMonth());
                processData.setPosMonth(ekartMaster.getPosMonth());
                processData.setRtoMonth(ekartMaster.getRtoMonth());
                processData.setRvpMonth(ekartMaster.getRvpMonth());
                processData.setDirectDeliveredMonth(ekartMaster.getDirectDeliveredMonth());
            }
            Calendar calendar = Calendar.getInstance();
            int currentMonthIndex = calendar.get(Calendar.MONTH);
//            if(currentMonthIndex==0){
//                currentMonthIndex=11;
//            }
            
            if (merchantMap.containsKey(processData.getClientId())) {
                String merchant = merchantMap.get(processData.getClientId()).getMerchantState();
                int rvpSurcharges= merchantMap.get(processData.getClientId()).getRvpSurcharges();
                processData.setMerchantState(merchant);
                processData.setRvpSurcharges(rvpSurcharges);
                if (stateMap.containsKey(merchant)) {
                    processData.setToPartyStateCode(stateMap.get(merchant).getStateCode());
                }

            } else {
                processData.setErrorMessage("Client ID is not present in Merchant database");
            }


            // Swapping the Source Pincode and Destination Pincode for RVP shipments.
            if(processData.getShipmentType().equalsIgnoreCase("RVP")){
                String swappin=processData.getSourcePincode();
                processData.setSourcePincode(processData.getDestinationPincode());
                processData.setDestinationPincode(swappin);
            }

            processData = preChecks(processData);


            /* this will be considered in next billing since shipment status date is not correct
            // Checking Shipment Status date for billing period.
            if(checkOutsideBillingPeriod(processData.getShipmentStatusDate()))
            {
                processData.setErrorMessage("Shipment is outside the billing period.");
            }
            */

            // RVP changes - Not billed
            if(processData.getShipmentType().equalsIgnoreCase("RVP") && (processData.getShipmentStatus().equalsIgnoreCase("Expected")
            || processData.getShipmentStatus().equalsIgnoreCase("Not_received") || processData.getShipmentStatus().equalsIgnoreCase("cancel_inititated") ||
                    processData.getShipmentStatus().equalsIgnoreCase("cancelled") || processData.getShipmentStatus().equalsIgnoreCase("unpicked_update")
                    || processData.getShipmentStatus().equalsIgnoreCase("out_for_pickup_update") || processData.getShipmentStatus().equalsIgnoreCase("dispatched_by_merchant")
                    ))
            {
                processData.setErrorMessage("RVP shipments - Not picked");
            }

            processData.setSourcePincode(String.valueOf((int)Double.parseDouble(processData.getSourcePincode())));
            processData.setDestinationPincode(String.valueOf((int)Double.parseDouble(processData.getDestinationPincode())));

            // Fetch source state and destination state from pincode master

            PincodeMaster sourcePincodeMaster = getDataByPincode(processData.getSourcePincode());

            if(sourcePincodeMaster !=null){
                String sourceState=sourcePincodeMaster.getState();
                processData.setSourceState(sourceState);
                processData.setSourceCity(sourcePincodeMaster.getCity());
                processData.setSourceZone(sourcePincodeMaster.getZone());
                processData.setSourceTier(sourcePincodeMaster.getTier());
                processData.setSourceZstate(sourcePincodeMaster.getZstate());
            }
            else{
                processData.setSourceState("N/A");
                processData.setErrorMessage("Source Pincode is blank or not present in the master");
            }

            PincodeMaster destinationPincodeMaster = getDataByPincode(processData.getDestinationPincode());
            if(destinationPincodeMaster != null){
                processData.setDestinationState(destinationPincodeMaster.getState().toUpperCase());
                processData.setDestinationCity(destinationPincodeMaster.getCity());
                processData.setDestinationZone(destinationPincodeMaster.getZone());
                processData.setDestinationTier(destinationPincodeMaster.getTier());
                processData.setDestinationZstate(destinationPincodeMaster.getZstate());
            }else{
                processData.setErrorMessage("Destination Pincode is blank or not present in the master");
            }

            processData.setSourceState(processData.getSourceState().toUpperCase());

            if (stateMap.containsKey(processData.getSourceState())) {
                processData.setFromPartyStateCode(stateMap.get(processData.getSourceState()).getStateCode());
            }



            // Zone-Mapping logic
            if (!nullOrEmptyCheck(processData.getSourceCity()) &&
                    !nullOrEmptyCheck(processData.getDestinationCity()) &&
                    !nullOrEmptyCheck(processData.getSourceZstate()) &&
                    !nullOrEmptyCheck(processData.getDestinationZstate()) &&
                    processData.getSourceCity().equals(processData.getDestinationCity()) &&
                    processData.getSourceZstate().equals(processData.getDestinationZstate())) {
                processData.setZone("LOCAL");
            } else if (!nullOrEmptyCheck(processData.getSourceZone()) &&
                    !nullOrEmptyCheck(processData.getDestinationZone()) &&
                    processData.getSourceZone().equals(processData.getDestinationZone())) {
                processData.setZone("ZONAL");
            } else if (!nullOrEmptyCheck(processData.getSourceTier()) &&
                    !nullOrEmptyCheck(processData.getDestinationTier()) &&
                    processData.getSourceTier().equals("METRO") &&
                    processData.getDestinationTier().equals("METRO")) {
                processData.setZone("METRO");
            } else if (!nullOrEmptyCheck(processData.getDestinationTier()) &&
                    (processData.getDestinationTier().equals("NEJK"))) {
                processData.setZone("NE J&K");
            } else {
                processData.setZone("ROI");
            }

            // Bill From state logic
            if(processData.getShipmentType().equalsIgnoreCase("Forward") || processData.getShipmentType().equalsIgnoreCase("RTO")){
                processData.setBillFromState(processData.getSourceState());
            }else{
                processData.setBillFromState(processData.getDestinationState());
            }

            if(processData.getBillFromState().equalsIgnoreCase("Nagaland") || processData.getBillFromState().equalsIgnoreCase("Manipur")
               || processData.getBillFromState().equalsIgnoreCase("Andaman & Nicobar Islands")) {
                if(processData.getShipmentType().equalsIgnoreCase("Forward") || processData.getShipmentType().equalsIgnoreCase("RTO")){
                    processData.setBillFromState(processData.getDestinationState());
                }else{
                    processData.setBillFromState(processData.getSourceState());
                }
            }

            if((processData.getSourceState().equalsIgnoreCase("Nagaland") || processData.getSourceState().equalsIgnoreCase("Manipur")
                    || processData.getSourceState().equalsIgnoreCase("Andaman & Nicobar Islands")) &&
                    (processData.getDestinationState().equalsIgnoreCase("Nagaland") || processData.getDestinationState().equalsIgnoreCase("Manipur")
                            || processData.getDestinationState().equalsIgnoreCase("Andaman & Nicobar Islands"))){
                    processData.setErrorMessage("Not registered in bill from state");
            }

            if (checkIfBlank(processData.getZone())) {
                processData.setErrorMessage("Zone is blank");
            }

            processData.setSellerVolumetricWeight(
                    Double.parseDouble(processData.getSellerDeclaredLength()) *
                            Double.parseDouble(processData.getSellerDeclaredBreadth()) *
                            Double.parseDouble(processData.getSellerDeclaredHeight()) / 5000
            );

            processData.setProfilerVolumetricWeight(
                    Double.parseDouble(processData.getProfilerLength()) *
                            Double.parseDouble(processData.getProfilerBreadth()) *
                            Double.parseDouble(processData.getProfilerHeight()) / 5000
            );

            // CASE 1

            if(Double.parseDouble(processData.getSellerDeadWeight()) <= 1.0 || Double.parseDouble(processData.getProfilerDeadWeight()) <= 1.0
                    || processData.getProfilerVolumetricWeight() > 3.5)
            {
                double max = Math.max(processData.getSellerVolumetricWeight(), Double.parseDouble(processData.getSellerDeadWeight()));
                max = Math.max(max, Double.parseDouble(processData.getProfilerDeadWeight()));

                processData.setBillableWeight(max) ;

            }
            // Assuming processData is an instance of the ProcessData class

            if(Double.parseDouble(processData.getSellerDeadWeight()) > 1.0 && Double.parseDouble(processData.getProfilerDeadWeight()) > 1.0
                    && processData.getProfilerVolumetricWeight() <= 3.5)
            {
                double max1 = Math.max(processData.getSellerVolumetricWeight(), Double.parseDouble(processData.getSellerDeadWeight()));
                double max2 = Math.max(processData.getProfilerVolumetricWeight(), Double.parseDouble(processData.getProfilerDeadWeight()));

                double max = Math.max(max1, max2);

                processData.setBillableWeight(max);

            }

            if (Objects.equals(processData.getClientId(), "CRM") || Objects.equals(processData.getClientId(), "NAP")  || Objects.equals(processData.getClientId(), "TOP") ||
                     Objects.equals(processData.getClientId(), "HOP") || Objects.equals(processData.getClientId(), "TDS")  || Objects.equals(processData.getClientId(), "WIQ") ||
                    Objects.equals(processData.getClientId(), "NUA")||Objects.equals(processData.getClientId(), "SGB")  || Objects.equals(processData.getClientId(), "SGF") ||
                    Objects.equals(processData.getClientId(), "ANS")||Objects.equals(processData.getClientId(), "ANA")  || Objects.equals(processData.getClientId(), "ANH") ||
                    Objects.equals(processData.getClientId(), "CUL")
                   ) {
                double maxNap = Math.max(processData.getSellerVolumetricWeight(), Double.parseDouble(processData.getSellerDeadWeight()));
                processData.setBillableWeight(maxNap);
            }

            if (Objects.equals(processData.getClientId(), "ILI") || Objects.equals(processData.getClientId(), "LMR")
                    || Objects.equals(processData.getClientId(), "PLN") || Objects.equals(processData.getClientId(), "XMI")
                    || Objects.equals(processData.getClientId(), "LHL") ) {
                processData.setBillableWeight(Double.parseDouble(processData.getSellerDeadWeight()));
            }

            if(Objects.equals(processData.getClientId(), "CLQ") )
            {
                if(Double.parseDouble(processData.getSellerDeadWeight())<=1.5){
                    processData.setBillableWeight(Double.parseDouble(processData.getSellerDeadWeight()));
                }else{
                    double maxNap = Math.max(processData.getSellerVolumetricWeight(), Double.parseDouble(processData.getSellerDeadWeight()));
                    processData.setBillableWeight(maxNap);
                }
            }



            processData.setBillableWeightKgs(String.valueOf(
                    processData.getBillableWeight() * 1000)
            );

            processData.setBillableWeightKgs(String.format("%.2f",
                    Double.parseDouble(processData.getBillableWeightKgs())
            ));

            processData.setBillableWeight(
                    Math.ceil(Double.parseDouble(processData.getBillableWeightKgs()))
            );

            processData.setMovementType(processData.getMovementType().toUpperCase());

            processData.setBillingZone(processData.getZone() + "-" + processData.getExpOrStd());

            int x = (int) (processData.getBillableWeight() / 1000);
            int y = (int) (processData.getBillableWeight()) % 1000;

            if (y == 0) {
                processData.setCeilWeight(String.valueOf(processData.getBillableWeight()));
            } else {
                if (y > 500) {
                    processData.setCeilWeight(String.valueOf((x * 1000) + 1000));
                } else {
                    processData.setCeilWeight(String.valueOf((x * 1000) + 500));
                }
            }

//            if(Objects.equals(processData.getClientId(), "PTM")&&processData.getBillableWeight()<=250){
//                processData.setCeilWeight(String.valueOf(250));
//            }
            if(weightRelatedMap.containsKey(processData.getClientId())){
                WeightRelated weightRelated=weightRelatedMap.get(processData.getClientId());
                if(Objects.equals(weightRelated.getReason(), "weight_250")){
                    if(processData.getBillableWeight()<=250){
                        processData.setCeilWeight(String.valueOf(250));
                    }
                }
            }

            processData.setZoneType(processData.getZone() + "-" + processData.getExpOrStd());


            if (Objects.equals(processData.getErrorMessage(), "No Error")) {
                List<String> additionalHead = new ArrayList<>();
                if (processData.getShipmentType().equalsIgnoreCase("Forward")) {
                    if (processData.getFwdMonth()!=null) {
                        LocalDateTime fwdMonthDate = LocalDateTime.parse(get20thDateOfMonth(processData.getFwdMonth() + 1, presentDate.getYear()));
                        if (presentDate.isAfter(fwdMonthDate)) {
                            if (processData.getShipmentStatus().equalsIgnoreCase("delivered")) {
                                if (processData.getDirectDeliveredMonth() !=null) {
                                    LocalDateTime directDeliveredMonthDate = LocalDateTime.parse(get20thDateOfMonth(processData.getDirectDeliveredMonth() + 1, presentDate.getYear()));
                                    if (presentDate.isAfter(directDeliveredMonthDate)) {
                                        processData.setM1M2Flag("M2");
                                        processData.setErrorMessage("Failed due to Multiple delivered status");
                                        isSaved = true;
                                        sendForSave(processData, "No Rate Required");
                                    } else {
                                        // Forward charges will be calculated.
                                        // Fuel surcharge + POS + COD will be calculated
                                        // Re-execution of Direct delivered

                                        if (processData.getPaymentType().equalsIgnoreCase("POS")) {
                                            additionalHead.add("POS Charge");
                                            additionalHead.add("COD charges");
                                        } else if (processData.getPaymentType().equalsIgnoreCase("COD")) {
                                            additionalHead.add("COD charges");
                                        }
                                        isSaved = true;
                                        callRate(processData, additionalHead, "");
                                    }
                                } else {
                                    processData.setM1M2Flag("M2");
                                    if (processData.getPosMonth() !=null) {
                                        LocalDateTime posMonthDate = LocalDateTime.parse(get20thDateOfMonth(processData.getPosMonth() + 1, presentDate.getYear()));
                                        if (presentDate.isAfter(posMonthDate)) {
                                            processData.setErrorMessage("Failed due to Multiple delivered status");
                                            isSaved = true;
                                            sendForSave(processData, "No Rate Required");
                                        } else {
                                            additionalHead.add("POS Charge");
                                            isSaved = true;
                                            callRate(processData, additionalHead, "");
                                        }
                                    } else {
                                        if(processData.getPaymentType().equalsIgnoreCase("POS")) {
                                            processData.setPosMonth(currentMonthIndex);
                                            additionalHead.add("POS Charge");
                                            isSaved = true;
                                            callRate(processData, additionalHead, "");
                                        }else{
                                            processData.setErrorMessage("Failed due to Multiple delivered status");
                                            isSaved = true;
                                            sendForSave(processData, "No Rate Required");
                                        }
                                    }
                                }
                            } else {
                                processData.setM1M2Flag("M2");
                                processData.setErrorMessage("Failed due to Multiple forward updates");
                                isSaved = true;
                                sendForSave(processData, "No Rate Required");
                            }
                        } else {
                            if (processData.getPaymentType().equalsIgnoreCase("POS")) {
                                additionalHead.add("POS Charge");
                                additionalHead.add("COD charges");
                            } else if (processData.getPaymentType().equalsIgnoreCase("COD")) {
                                additionalHead.add("COD charges");
                            }
                            isSaved = true;
                            callRate(processData, additionalHead, "");
                        }
                    } else {
                        processData.setFwdMonth(currentMonthIndex);
                        if (processData.getPaymentType().equalsIgnoreCase("POS")) {
                            processData.setDirectDeliveredMonth(currentMonthIndex);
                            additionalHead.add("POS Charge");
                            additionalHead.add("COD charges");
                        } else if (processData.getPaymentType().equalsIgnoreCase("COD")) {
                            additionalHead.add("COD charges");
                        }
                        isSaved = true;
                        callRate(processData, additionalHead, "");
                    }
                }

                if (processData.getShipmentType().equalsIgnoreCase("RTO")) {
                    if (processData.getRtoMonth() !=null) {
                        LocalDateTime rtoMonthDate = LocalDateTime.parse(get20thDateOfMonth(processData.getRtoMonth() + 1, presentDate.getYear()));
                        if (presentDate.isAfter(rtoMonthDate)) {
                            processData.setM1M2Flag("M2");
                            processData.setErrorMessage("Failed due to Multiple RTO status");
                            isSaved = true;
                            sendForSave(processData, "No Rate Required");
                        } else {
                            if (processData.getFwdMonth() !=null) {
                                processData.setM1M2Flag("M2");
                                additionalHead.add("COD charges");
                                isSaved = true;
                                callRate(processData, additionalHead, "NEGATIVE_COD");
                            } else {
                                isSaved = true;
                                callRate(processData, additionalHead, "");
                                ProcessData rtoData = new ProcessData(processData);
                                rtoData.setShipmentType("FORWARD");
                                callRate(rtoData, additionalHead, "RTO_FORWARD");
                            }
                        }
                    } else {
                        processData.setRtoMonth(currentMonthIndex);
                        if (processData.getFwdMonth() !=null) {
                           LocalDateTime fwdMonthDate1 = LocalDateTime.parse(get20thDateOfMonth(processData.getFwdMonth() + 1, presentDate.getYear()));
                            if(presentDate.isAfter(fwdMonthDate1)){
                            processData.setM1M2Flag("M2");
                            additionalHead.add("COD charges");
                            isSaved = true;
                            callRate(processData, additionalHead, "NEGATIVE_COD");
                            }
                            else
                            {
                                processData.setM1M2Flag("M1");
                                isSaved = true;
                                callRate(processData, additionalHead, "");
                                ProcessData rtoData = new ProcessData(processData);
                                rtoData.setShipmentType("FORWARD");
                                callRate(rtoData, additionalHead, "RTO_FORWARD");

                            }
                        } else {
                            isSaved = true;
                            callRate(processData, additionalHead, "");
                            ProcessData rtoData = new ProcessData(processData);
                            rtoData.setShipmentType("FORWARD");
                            callRate(rtoData, additionalHead, "RTO_FORWARD");
                        }
                    }
                }

                if (processData.getShipmentType().equalsIgnoreCase("RVP")) {
                    if (processData.getRvpMonth() !=null) {
                        LocalDateTime rvpMonthDate = LocalDateTime.parse(get20thDateOfMonth(processData.getRvpMonth() + 1, presentDate.getYear()));
                        if (presentDate.isAfter(rvpMonthDate)) {
                            processData.setM1M2Flag("M2");
                            processData.setErrorMessage("Failed due to Multiple RVP status");
                            isSaved = true;
                            sendForSave(processData, "No Rate Required");
                        } else {
                            isSaved = true;
                            callRate(processData, additionalHead, "");
                        }
                    } else {
                        processData.setRvpMonth(currentMonthIndex);
                        isSaved = true;
                        callRate(processData, additionalHead, "");
                    }
                }
            }

            if (!processData.getErrorMessage().equals("No Error")) {
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
                System.out.println("Error Message : " + processData.getErrorMessage());
            }
        }catch(Exception exception){
            log.error(exception);
            log.error("Error in Raw processing for reference: "+processData.getTrackingId());
            exception.printStackTrace();
        }
        if(!isSaved) {
            sendForSave(processData, "No Rate Required");
        }
    }

    private boolean nullOrEmptyCheck(String str) {
        return str == null || str.trim().isEmpty();
    }

    private ProcessData preChecks(ProcessData processDataInput){
        ProcessData processData = new ProcessData(processDataInput);
        try{
            if("LHL".equalsIgnoreCase(processData.getClientId()) || "BRP".equalsIgnoreCase(processData.getClientId())){
                if (checkIfBlank(processData.getMhInscanDate()) && !"RVP".equalsIgnoreCase(processData.getShipmentType())) {
                    processData.setErrorMessage("MHInscan Date is blank");
                }
            }else{
                if (checkIfBlank(processData.getPickupDate()) && !"RVP".equalsIgnoreCase(processData.getShipmentType())) {
                    processData.setErrorMessage("Pick up Date is blank");
                }
            }

            if (checkIfBlank(processData.getClientId())) {
                processData.setErrorMessage("Client Id is not present");
            }
            if (checkIfBlank(processData.getShipmentType())) {
                processData.setErrorMessage("Shipment Type is blank");
            }
            if (checkIfBlank(processData.getShipmentStatus())) {
                processData.setErrorMessage("Shipment Status is blank");
            }

            if (!checkIfBlank(processData.getClientId()) && "BAL".equalsIgnoreCase(processData.getClientId())
                    && !("delivered".equalsIgnoreCase(processData.getShipmentStatus()))) {
                processData.setErrorMessage("Open shipments of Airtel");
            }
//            if (checkIfBlank(processData.getZone())) {
//                processData.setErrorMessage("Zone is blank");
//            }
            if (checkIfBlank(processData.getMovementType())) {
                processData.setErrorMessage("Movement Type is blank");
            }
            if (checkIfBlank(processData.getPaymentType())) {
                processData.setErrorMessage("Payment Type is blank");
            }
            if (checkIfBlank(processData.getSellerDeadWeight())) {
                processData.setSellerDeadWeight("0");
            }
            if (checkIfBlank(processData.getProfilerDeadWeight())) {
                processData.setProfilerDeadWeight("0");
            }
            if (checkIfBlank(processData.getSellerDeclaredLength())) {
                processData.setSellerDeclaredLength("0");
            }
            if (checkIfBlank(processData.getSellerDeclaredBreadth())) {
                processData.setSellerDeclaredBreadth("0");
            }
            if (checkIfBlank(processData.getSellerDeclaredHeight())) {
                processData.setSellerDeclaredHeight("0");
            }
            if (checkIfBlank(processData.getProfilerLength())) {
                processData.setProfilerLength("0");
            }
            if (checkIfBlank(processData.getProfilerBreadth())) {
                processData.setProfilerBreadth("0");
            }
            if (checkIfBlank(processData.getProfilerHeight())) {
                processData.setProfilerHeight("0");
            }
            if (checkIfBlank(processData.getSourcePincode())) {
                processData.setSourcePincode("0");
            }
            if (checkIfBlank(processData.getDestinationPincode())) {
                processData.setDestinationPincode("0");
            }

            if (checkIfBlank(processData.getAmountToCollect())) {
                processData.setAmountToCollect("0");
            }

            //Casting shipment type to Upper case
            processData.setShipmentType(processData.getShipmentType().toUpperCase());

            if ("JKNE".equalsIgnoreCase(processData.getZone())) {
                processData.setZone("NE J&K");
            }


            if ("POS".equals(processData.getPaymentType()) && !"delivered".equals(processData.getShipmentStatus())) {
                processData.setPaymentType("COD");
            }

            if (!("FORWARD".equals(processData.getShipmentType()) || "RTO".equals(processData.getShipmentType()) || "RVP".equals(processData.getShipmentType()))) {
                processData.setErrorMessage("Shipment Type is not valid. It can only be of Forward, RTO, and RVP Type");
            }

            if (!("PREPAID".equals(processData.getPaymentType()) || "PP".equals(processData.getPaymentType()) || "COD".equals(processData.getPaymentType()) || "POS".equals(processData.getPaymentType()))) {
                processData.setErrorMessage("Please provide correct Payment Type");
            }

            if ("REGULAR".equals(processData.getMovementType())) {
                processData.setExpOrStd("EXP");
            } else {
                processData.setExpOrStd("STD");
            }

            if ("FORWARD".equals(processData.getShipmentType()) || "RTO".equals(processData.getShipmentType())) {
                processData.setEntityType("Outgoing");
            }

            if ("RVP".equals(processData.getShipmentType())) {
                processData.setEntityType("Incoming");
            }

            if ("PREPAID".equals(processData.getPaymentType()) || "PP".equals(processData.getPaymentType())) {
                processData.setPaymentMode("PP");
            }

            if ("COD".equals(processData.getPaymentType()) || "POS".equals(processData.getPaymentType())) {
                processData.setPaymentMode("COD");
            }
        }catch(Exception e){
            log.error(e.getMessage());
            log.error("Error in pre checks for reference: "+processData.getTrackingId());
        }
        return processData;
    }


    private Map<String, MerchantMaster> getMerchantMap() {
        Map<String, MerchantMaster> merchantMap = new HashMap<>();


        try {
            List<MerchantMaster> merchantList = merchantMasterDao.findAll();
            for (MerchantMaster merchant : merchantList) {
                merchantMap.put(merchant.getClientId(), merchant);
            }
            return merchantMap;
        } catch (Exception e) {
            log.error("Error in merchant map:"+e.getMessage());
            e.printStackTrace();
        }
        return merchantMap;
    }

    private Map<String, WeightRelated> getWeightRelated() {
        Map<String, WeightRelated> weightRelatedMap = new HashMap<>();


        try {
            List<WeightRelated> weightRelatedList = weightRelatedMasterDao.findAll();
            for (WeightRelated weightRelated : weightRelatedList) {
                weightRelatedMap.put(weightRelated.getClientId(), weightRelated);
            }
            return weightRelatedMap;
        } catch (Exception e) {
            log.error("Error in weight related map:"+e.getMessage());
            e.printStackTrace();
        }
        return weightRelatedMap;
    }


    private Map<String, StateMasterDto> getstateMap() {
        Map<String, StateMasterDto> stateMap = new HashMap<>();
        try {
            InputStream inputStream = EkartRawDataConsumer.class.getClassLoader().getResourceAsStream("State_Code_Master.json");

            if (inputStream != null) {
                List<StateMasterDto> merchantList = objectMapper.readValue(inputStream, new TypeReference<List<StateMasterDto>>() {});
                // Convert the list to a Map
                stateMap = merchantList.stream().collect(Collectors.toMap(StateMasterDto::getStateName, dto -> dto));
                return stateMap;
            }
        } catch (Exception e) {
            log.error("Error in state map:"+e.getMessage());
            e.printStackTrace();
        }
        return stateMap;
    }

    private EkartMaster getDataByTrackingId(String trackingId) {
        return ekartMasterDao.findByTrackingId(trackingId);
    }
    private PincodeMaster getDataByPincode(String pincode) {
        return pincodeMasterDao.findByPincode(pincode);
    }


    private boolean checkIfBlank(String field) {
        return field == null || field.isEmpty() || field.equals("N/A") || field.equals("#N/A") || field.equals("NULL") || field.equalsIgnoreCase("n/a") || field.equals("0");
    }

//    public static String get20thDateOfMonth(int monthIndex, int year ) {
//        if(monthIndex>11){
//            monthIndex = monthIndex -12 ;
//        }
//        if (monthIndex >= 0 && monthIndex <= 11) {
//            Calendar calendar = Calendar.getInstance();
//            calendar.set(year, monthIndex, 20);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            return sdf.format(calendar.getTime()).replace("T", " ");
//        } else {
//            return "Invalid month index. Please provide a number between 0 and 11.";
//        }
//    }

    /*
    public static boolean checkOutsideBillingPeriod(String shipmentStatusDate) {
        String[] date_time= shipmentStatusDate.split("\\s+");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[M]M/[d]d/yyyy");
        LocalDateTime parsedDate = LocalDateTime.parse(date_time[0], formatter).toLocalDate().atStartOfDay();

        LocalDate currentDate = LocalDate.now();

        LocalDate startDate = currentDate.withDayOfMonth(26).minusMonths(1);
//        if (startDate.getMonthValue() == 12) {
//            startDate = startDate.withYear(startDate.getYear() - 1);
//        }

        LocalDate endDate = currentDate.withDayOfMonth(25);

        return !parsedDate.isAfter(startDate.atStartOfDay()) || !parsedDate.isBefore(endDate.atStartOfDay());

    }
    */

    public static String get20thDateOfMonth(int monthIndex, int year) {


        if(monthIndex>11){
            monthIndex = monthIndex -12 ;
//            year=year+1;
        }
        if(monthIndex >= 7 && monthIndex <= 11){
            year = year-1;
        }
        if (monthIndex >= 0 && monthIndex <= 11) {
            // Month values in Java 8 start from 1, not 0, so increment the monthIndex by 1
            Month month = Month.of(monthIndex + 1);

            LocalDateTime dateTime = LocalDateTime.of(year, month, 20, 0, 0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            return dateTime.format(formatter);
        } else {
            return "Invalid month index. Please provide a number between 0 and 11.";
        }
    }

    public static LocalDateTime getDateTimeInZone(String timeZone) {
        return LocalDateTime.now(ZoneId.of(timeZone));
    }

    public void callRate(ProcessData processData, List<String> additionalHead, String flowCode) {
        try {
            List<String> additionalCharges = new ArrayList<>(additionalHead);
            int weight = (int)(processData.getBillableWeight());

            additionalCharges.add("Fuel Surcharge Std");
            additionalCharges.add("Fuel Surcharge Exp");
            additionalCharges.add("Fuel Surcharge");

            String shipmentType = processData.getShipmentType();

            RateCalculationRequestDTO rateCalculationRequestPayload = new RateCalculationRequestDTO();
            rateCalculationRequestPayload.setParty(processData.getClientId());
            rateCalculationRequestPayload.setOrigin(processData.getBillingZone());
            rateCalculationRequestPayload.setDestination(processData.getBillingZone());
            rateCalculationRequestPayload.setSameCity(false);
            rateCalculationRequestPayload.getRateCalculationProcessRequestDTO().setReferenceNumber(processData.getTrackingId());
            rateCalculationRequestPayload.getRateCalculationProcessRequestDTO().setProcessMasterCode(processData);
            rateCalculationRequestPayload.getRateCalculationProcessRequestDTO().setCompanyId(Integer.parseInt(configProperty.getEkartCompanyId()));
            rateCalculationRequestPayload.getRateCalculationProcessRequestDTO().setTimeZone("Asia/Kolkata");
            rateCalculationRequestPayload.getRateCalculationProcessRequestDTO().setFlowCode(flowCode);
            rateCalculationRequestPayload.setOrderTypeCode("default");
            rateCalculationRequestPayload.setBookingDate(processData.getBookingDateTime());
            rateCalculationRequestPayload.setItemValue((int)Double.parseDouble(processData.getShipmentValue()));
            rateCalculationRequestPayload.setCodAmount((int)Double.parseDouble(processData.getAmountToCollect()));
            rateCalculationRequestPayload.setWeight(weight);
            rateCalculationRequestPayload.setStorageDays(3.0);
            rateCalculationRequestPayload.setResponseTopic(configProperty.getEkartRateTopic());
            rateCalculationRequestPayload.setAdditionalHeads(additionalCharges);
            rateCalculationRequestPayload.setServiceType(shipmentType);


            String key = UUID.randomUUID().toString();
            ProducerRecord<String, String> producerRecord = null;
            producerRecord = new ProducerRecord<>(configProperty.getEkartRateSendTopic(), key, objectMapper.writeValueAsString(rateCalculationRequestPayload));
            kafkaProducer.send(producerRecord);

            log.error("Tracking Id sent for Tax Calculation: "+processData.getTrackingId());

        } catch (Exception ex) {
            // Handle the exception or log as required
            log.error("Error in Rate Call:"+ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void sendForSave(ProcessData processData, String flowCode) {
        try {
            EkartRateDto ekartRateDto = new EkartRateDto();
            EkartRateDto.RateDto rateDto = new EkartRateDto.RateDto();
            rateDto.setFlowCode(flowCode);
            rateDto.setReferenceNumber(processData.getTrackingId());
            rateDto.setProcessMasterCode(processData);
            ekartRateDto.setRateCalculationProcessRequestDTO(rateDto);

            String key = UUID.randomUUID().toString();
            ProducerRecord<String, String> producerRecord = null;
            producerRecord = new ProducerRecord<>(configProperty.getEkartRateTopic(), key, objectMapper.writeValueAsString(ekartRateDto));
            producerRecord.headers().add("schema_name", configProperty.getEkartSchemaName().getBytes(StandardCharsets.UTF_8));
            kafkaProducer.send(producerRecord);

            log.error("Tracking Id sent for save due to error: "+processData.getTrackingId());

        } catch (Exception ex) {
            // Handle the exception or log as required
            ex.printStackTrace();
            log.error("Error in Send for Save : " + ex.getMessage());
        }
    }



}
