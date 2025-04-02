package co.fareye.service;

import co.fareye.config.ConfigProperty;
import co.fareye.config.security.GatewayUser;
import co.fareye.constant.AppConstant;
import co.fareye.dao.EkartMasterDao;
import co.fareye.dto.ProcessData;
import co.fareye.dto.fileupload.DirectoryAndFileExtension;
import co.fareye.dto.fileupload.FileDetail;
import co.fareye.dto.fileupload.FileDownloadConfiguration;
import co.fareye.dto.fileupload.FileHandlingProtocolAuth;
import co.fareye.model.EkartMaster;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Log4j2
public class UploadService {

    @Autowired
    EkartMasterDao ekartMasterDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MinioService bucketClient;

    @Autowired
    private ConfigProperty configProperty;

    @Autowired
    @Qualifier("StringKeyValueProducer")
    KafkaProducer kafkaProducer;


    private static ThreadLocal<List<String>> headerThreadLocal = new ThreadLocal<>();

    private static final int page_size = 1500;

    public ResponseEntity<Object> uploadSuccess(String clientId) {
        String successUpload = "";
        String failedUpload = "";
        try {
            log.error("Success Upload started for client : " + clientId);
            successUpload = uploadData(clientId, true,"ekart_" + clientId + "_" + "success_" + LocalDateTime.now() + "_.csv",configProperty.getUploadDirectory());
            log.error("Upload Success Status: " + successUpload);

            log.error("Failed Upload started for client : " + clientId);
            failedUpload = uploadData(clientId, false,"ekart_" + clientId + "_" + "failed_" + LocalDateTime.now() + "_.csv",configProperty.getUploadFailedDirectory());
            log.error("Upload Failed Status: " + failedUpload);

            return new ResponseEntity<>("Success Upload: "+successUpload + " Failed Upload: "+failedUpload, HttpStatus.OK);
        }catch(Exception ex){
            log.error("Upload-Error overall: "+ex);
            return new ResponseEntity<>("Success Upload: "+successUpload + " Failed Upload: "+failedUpload, HttpStatus.NOT_ACCEPTABLE);
        }
    }
    private String uploadData(String clientId,boolean isUpdated,String fileName,String uploadDirectory){
        String result = null;
        boolean dataAvailable = true;
        int i = 0;
        String dataFileName = "file-upload-stream-" + Thread.currentThread().getId() + ".csv";
        Map<String, String> metaData = new HashMap<>();
        metaData.put("fileName", fileName);
        JsonNode rawDataLinkResponse = null;
        try {
            while (dataAvailable) {
                List<Map<String,Object>> ekartMasterMap = ekartMasterDao.findByIsUpdatedAndIsProcessedTrueAndClientIdAndIntFileStatus(isUpdated, clientId);
                List<JsonNode> uploadData = new ArrayList<>();
                if (ekartMasterMap.isEmpty()) {
                    rawDataLinkResponse = writeDataToFileInChunks(dataFileName, uploadData, true, metaData);
                    dataAvailable = false;
                    break;
                }
                log.info(i);
                uploadData = mapUploadData(ekartMasterMap);
                writeDataToFileInChunks(dataFileName, uploadData, false, metaData);
//                updateInDB(ekartMasterList);
            }

            if (rawDataLinkResponse.has("rawDataLink")) {
                pushDataToUploadTopic(rawDataLinkResponse,uploadDirectory);
                result = "Completed for client: " + clientId;
            } else {
                result = "Failed to upload for client " + clientId;
            }
            return result;
        } catch (Exception e) {
            log.error("Upload-Error: " + e);
            result = e.getMessage();
            return result;
        }
    }
    private void pushDataToUploadTopic(JsonNode rawDataLinkResponse,String uploadDirectory) throws JsonProcessingException, ExecutionException, InterruptedException {
        try {
            String topic = "int" + AppConstant.HYPHEN + configProperty.getEnvironment() + AppConstant.HYPHEN + "fareye-upload-file" + AppConstant.HYPHEN + "1" + AppConstant.HYPHEN + "topic";
            FileHandlingProtocolAuth auth = FileHandlingProtocolAuth.builder()
                    .username(configProperty.getSftpUsername())
                    .password(configProperty.getSftpPassword())
                    .choice("password")
                    .build();
            FileDetail fileDetail = FileDetail.builder()
                    .fileExtension("csv")
                    .columnSeparator(",")
                    .fileHasHeader("yes")
                    .batchSize(configProperty.getSftpUploadBatchSize())
                    .header(null)
                    .build();
            DirectoryAndFileExtension directoryAndFileExtension = DirectoryAndFileExtension.builder()
                    .currentDirectory(uploadDirectory)
                    .currentFileExtension("csv")
                    .fileDetail(Collections.singletonList(fileDetail))
                    .build();
            FileDownloadConfiguration fileDownloadConfiguration = FileDownloadConfiguration.builder()
                    .port(configProperty.getSftpPort())
                    .fileServiceType("FILE_SAVE_UPLOAD")
                    .server("SFTP")
                    .host(configProperty.getSftpHost())
                    .port(configProperty.getSftpPort())
                    .connectionCount(1)
                    .connectTimeout(20)
                    .serviceCode("rate-estimation-upload")
                    .connectorCode("ekart")
                    .auth(auth)
                    .s3FileDataLink(rawDataLinkResponse.get("rawDataLink").asText())
                    .directoryAndExtension(directoryAndFileExtension)
                    .host(configProperty.getSftpHost())
                    .user(GatewayUser.getUser())
                    .build();
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, UUID.randomUUID().toString(), objectMapper.writeValueAsString(fileDownloadConfiguration));
            kafkaProducer.send(producerRecord).get();
        }catch(Exception ex){
            log.error("Upload-Error in pushing to file service: "+ex);
        }
    }

    private ObjectNode writeDataToFileInChunks(String fileName, List<JsonNode> ekartMasterList, boolean isLastBatch, Map<String, String> metaData) throws IOException {
        File file = new File("/tmp/" + fileName);
        boolean fileExist = file.exists();
        CsvWriter writer = null;
        String dataLink = null;
        String data = objectMapper.writeValueAsString(ekartMasterList);
        try (JsonParser jsonParser = objectMapper.getFactory().createParser(data);
             FileWriter fileWriter = new FileWriter(file, true)) {
            CsvWriterSettings csvWriterSettings = new CsvWriterSettings();
            csvWriterSettings.setNullValue("");
            csvWriterSettings.setEmptyValue("");
            csvWriterSettings.getFormat().setLineSeparator("\n");
            if (!fileExist) {
                setHeaders(data);
                writer = new CsvWriter(fileWriter, csvWriterSettings);
                writer.writeRow(headerThreadLocal.get());
            } else writer = new CsvWriter(fileWriter, csvWriterSettings);
            writeRows(writer, jsonParser);
        } catch (Exception e) {
            log.error("Exception writing file ", e);
        } finally {
            if (writer != null)
                writer.close();
        }
        if (isLastBatch) {
            String md5 = DigestUtils.md5DigestAsHex(Files.newInputStream(file.toPath()));
            String updatedFileName = FilenameUtils.removeExtension(fileName) + md5 + ".csv";
            String fileCompletePath = "file-upload-stream-" + configProperty.getEnvironment() + AppConstant.PATH_SEPARATOR + GatewayUser.getUser().getOrganizationId() + AppConstant.PATH_SEPARATOR + AppConstant.PATH_SEPARATOR + updatedFileName;
            dataLink = bucketClient.uploadFile(fileCompletePath, file, configProperty.getS3BucketName(), metaData);
            file.delete();
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.put("rawDataLink", dataLink);
        return result;
    }

    private void setHeaders(String data) throws IOException {
        List<String> headers = new ArrayList<>();
        JsonParser jsonParser = objectMapper.getFactory().createParser(data);
        jsonParser.nextToken();
        while (!jsonParser.isClosed()) {
            jsonParser.nextToken();
            JsonNode objectNode = objectMapper.readTree(jsonParser);
            if (objectNode != null && objectNode.isObject()) {
                objectNode.fieldNames().forEachRemaining(key -> {
                    if (!headers.contains(key)) {
                        headers.add(key);
                    }
                });
            }
        }
        headerThreadLocal.set(headers);
    }

    private void writeRows(CsvWriter writer, JsonParser jsonParser) throws IOException {
        if (jsonParser.nextToken() == JsonToken.START_ARRAY) {
            while (!jsonParser.isClosed()) {
                jsonParser.nextToken();
                JsonNode data = objectMapper.readTree(jsonParser);
                if (data != null && !data.isNull()) {
                    List<Object> rowData = new ArrayList<>();
                    for (String header : headerThreadLocal.get()) {
                        JsonNode value = data.get(header);
                        if (value != null)
                            rowData.add(value.asText());
                        else rowData.add(null);
                    }
                    writer.writeRow(rowData);
                }
            }
        } else throw new RuntimeException("Expecting a batch of data to proceed with the streaming");
    }

    private Pageable preparePageReq(int pageNo) {
        return PageRequest.of(pageNo, page_size);
    }

    private void updateAndInsertInDB(List<EkartMaster> ekartMasterList) {
        //update the status "UPLOADED"
        ekartMasterList.forEach(processData -> {

        });

    }

    private boolean updateInDB(List<EkartMaster> ekartMasterList) {
        ekartMasterDao.saveAll(ekartMasterList);
        return true;

    }

    private List<JsonNode> mapUploadData(List<Map<String,Object>> ekartMasterMap){
        List<JsonNode> uploadList = new ArrayList<>();
        try {
            ekartMasterMap.forEach(ele -> {

                try {
                    ProcessData processData = objectMapper.readValue(ele.get("data").toString(), ProcessData.class);
                    ObjectNode uploadData = objectMapper.createObjectNode();
                    uploadData.put("tracking_id", processData.getTrackingId());
                    uploadData.put("merchant_id", getValueOrDefault(processData.getMerchantId()));
                    uploadData.put("pickup_date", getValueOrDefault(processData.getPickupDate()));
                    uploadData.put("mh_inscan_date", getValueOrDefault(processData.getMhInscanDate()));
                    uploadData.put("accrual_party_id_to", getValueOrDefault(processData.getClientId()));
                    uploadData.put("accrual_party_id_from", getValueOrDefault(processData.getAccrualPartyIdFrom()));
                    uploadData.put("shipment_status", getValueOrDefault(processData.getShipmentStatus()));
                    uploadData.put("accrual_group", getValueOrDefault(processData.getAccuralGroup()));
                    uploadData.put("shipment_type", getValueOrDefault(processData.getShipmentType()));
                    uploadData.put("zone", getValueOrDefault(processData.getZone()));
                    uploadData.put("zone_type", getValueOrDefault(processData.getZoneType()));
                    uploadData.put("billeable_weight", getValueOrDefault(String.valueOf(processData.getBillableWeight())));
                    uploadData.put("ceil_weight", getValueOrDefault(processData.getCeilWeight()));
                    uploadData.put("payment_type", getValueOrDefault(processData.getPaymentType()));
                    uploadData.put("payment_mode", getValueOrDefault(processData.getPaymentMode()));
                    uploadData.put("movement_type", getValueOrDefault(processData.getMovementType()));
                    uploadData.put("FWD_Revenue", getValueOrDefault(String.valueOf(processData.getFwdShippingRevenue())));
                    uploadData.put("RTO_Revenue", getValueOrDefault(String.valueOf(processData.getRtoRevenue())));
                    uploadData.put("RVP_Revenue", getValueOrDefault(String.valueOf(processData.getRvpRevenue())));
                    uploadData.put("COD_Revenue", getValueOrDefault(String.valueOf(processData.getCodRevenue())));
                    uploadData.put("POS_Revenue", getValueOrDefault(String.valueOf(processData.getPosRevenue())));
           //         uploadData.put("total_COD_POS", getValueOrDefault(String.valueOf(processData.getCodPosRevenue())));
                    uploadData.put("fuel_surcharges", getValueOrDefault(String.valueOf(processData.getFuelSurcharges())));
                    uploadData.put("Gross_shipping", getValueOrDefault(String.valueOf(processData.getGrossShipping())));
                    uploadData.put("Gross_COD_POS", getValueOrDefault(String.valueOf(processData.getCodPosRevenue())));
                    uploadData.put("Gross_revenue", getValueOrDefault(String.valueOf(processData.getGrossRevenue())));
                    uploadData.put("Shipping_discount", getValueOrDefault(String.valueOf(processData.getShippingDiscount())));
                    uploadData.put("COD_discount", getValueOrDefault(String.valueOf(processData.getCodDiscount())));
                    uploadData.put("total_discount", getValueOrDefault(String.valueOf(processData.getTotalDiscount())));
                    uploadData.put("Net_shipping", getValueOrDefault(String.valueOf(processData.getNetShipping())));
                    uploadData.put("Net_COD", getValueOrDefault(String.valueOf(processData.getNetCod())));
                    uploadData.put("Net Revenue", getValueOrDefault(String.valueOf(processData.getNetRevenue())));

   //               uploadData.put("total_revenue", getValueOrDefault(String.valueOf(processData.getTotalRevenue())));
                    uploadData.put("total_tax", getValueOrDefault(String.valueOf(processData.getTotalTax())));
                    uploadData.put("Total_amt", getValueOrDefault(String.valueOf(processData.getTotalRevenueTax())));
                    uploadData.put("amount_to_collect", getValueOrDefault(processData.getAmountToCollect()));
                    uploadData.put("source_pincode", getValueOrDefault(processData.getSourcePincode()));
                    uploadData.put("source_state", getValueOrDefault(processData.getSourceState()));
                    uploadData.put("from_party_state_code", getValueOrDefault(processData.getFromPartyStateCode()));
                    uploadData.put("destination_pincode", getValueOrDefault(processData.getDestinationPincode()));
                    uploadData.put("destination_state", getValueOrDefault(processData.getDestinationState()));
                    uploadData.put("to_party_state_code", getValueOrDefault(processData.getToPartyStateCode()));
                    uploadData.put("cgst_tax_rate", getValueOrDefault(processData.getCgstTaxRate()));
                    uploadData.put("igst_tax_rate", getValueOrDefault(processData.getIgstTaxRate()));
                    uploadData.put("sgst_tax_rate", getValueOrDefault(processData.getSgstTaxRate()));
                    uploadData.put("cgst_tax_amount", getValueOrDefault(String.valueOf(processData.getCgstTotalAmount())));
                    uploadData.put("igst_tax_amount", getValueOrDefault(String.valueOf(processData.getIgstTotalAmount())));
                    uploadData.put("sgst_tax_amount", getValueOrDefault(String.valueOf(processData.getSgstTotalAmount())));
                    uploadData.put("value", getValueOrDefault(processData.getValue()));
                    uploadData.put("entity_type", getValueOrDefault(processData.getEntityType()));
                    uploadData.put("sac_code", getValueOrDefault(String.valueOf(processData.getSacCode())));
                    uploadData.put("error_message", getValueOrDefault(processData.getErrorMessage()));
                    uploadData.put("m1_m2_flag", getValueOrDefault(processData.getM1M2Flag()));
                    uploadData.put("merchant_state", getValueOrDefault(processData.getMerchantState()));
                    uploadData.put("Bill from State", getValueOrDefault(processData.getBillFromState()));
                    uploadList.add(uploadData);
                }catch (Exception ex){
                    log.error("Exception in mapping : "+ex);
                }
            });
        }catch (Exception e){
            log.error("Error in mapping: "+e);
        }
        return uploadList;
    }

    private String getValueOrDefault(String value) {
        return value != null ? value : "";
    }

}