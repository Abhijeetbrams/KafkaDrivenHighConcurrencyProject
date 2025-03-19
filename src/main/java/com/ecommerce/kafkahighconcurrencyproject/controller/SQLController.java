package com.ecommerce.kafkahighconcurrencyproject.controller;

import com.ecommerce.kafkahighconcurrencyproject.dao.EkartMasterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/public/api/sql")
public class SQLController {

    @Autowired
    EkartMasterDao ekartMasterDao;

    @Autowired
    SQLService sqlService;

    @GetMapping("/count")
    public ResponseEntity getCountOfIsProcessed() {
        long countOfTrackingId = ekartMasterDao.countByIsProcessedTrue();

        return new ResponseEntity<>("Count of TrackingId: "+ countOfTrackingId, HttpStatus.OK);
    }

    @GetMapping("/rto-tracking-ids-csv")
    public void getTrackingIdsRTOCsv(HttpServletResponse response, @RequestParam int rtoMonth) throws IOException {
        List<String> trackingIds=ekartMasterDao.findTrackingIdsByRtoMonth(rtoMonth);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"tracking_ids.csv\"");
        sqlService.writeTrackingIdsToCsv(response.getWriter(),trackingIds);
    }

    @GetMapping("/rto-fwd-tracking-ids-csv")
    public void getTrackingIdsFWDRTOCsv(HttpServletResponse response, @RequestParam int rtoMonth) throws IOException {
        List<String> trackingIds=ekartMasterDao.findTrackingIdsByFWDRtoMonth(rtoMonth);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"tracking_ids.csv\"");
        sqlService.writeTrackingIdsToCsv(response.getWriter(),trackingIds);
    }

    @GetMapping("/rto-nff-tracking-ids-csv")
    public void getTrackingIdsNFFRTOCsv(HttpServletResponse response, @RequestParam int rtoMonth) throws IOException {
        List<String> trackingIds=ekartMasterDao.findTrackingIdsByRtoMonthAndErrorMessage(rtoMonth);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"tracking_ids.csv\"");
        sqlService.writeTrackingIdsToCsv(response.getWriter(),trackingIds);
    }

    @GetMapping("/pincode-tracking-ids-csv")
    public void getTrackingIdspPincodeCsv(HttpServletResponse response) throws IOException {
        List<String> trackingIds=ekartMasterDao.findTrackingIdsByPincodeErrorMessages();
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"tracking_ids.csv\"");
        sqlService.writeTrackingIdsToCsv(response.getWriter(),trackingIds);
    }

    @GetMapping("/client-fwd-counts")
    public List<ClientCountDTO> getCountOfIsFWDProcessed() {
        return ekartMasterDao.countByClientIdAndFWDCriteria();
    }

    @GetMapping("/client-rto-counts")
    public List<ClientCountDTO> getCountOfIsRTOProcessed() {
        return ekartMasterDao.countByClientIdAndRTOCriteria();
    }

    @GetMapping("/client-rvp-counts")
    public List<ClientCountDTO> getCountOfIsRVPProcessed() {
        return ekartMasterDao.countByClientIdAndRVPCriteria();
    }

    @GetMapping("/client-error-counts")
    public List<ClientCountDTO> getCountOfClientIdProcessed() {
        return ekartMasterDao.countByClientIdForErrorMessage();
    }

    @GetMapping("/distinct-error-message")
    public List<ErrorMessageCountDTO> getCountOfDistinctErrorMessage() {
        return ekartMasterDao.countByDistinctErrorMessage();
    }

    // Update queries
    @PutMapping("/update-is-processed")
    public ResponseEntity<String> updateIsProcessed() {
        int updatedRows = ekartMasterDao.updateIsProcessed();
        return new ResponseEntity<>("Updated rows: " + updatedRows, HttpStatus.OK);
    }


    @PutMapping("/update-int-file-status")
    public ResponseEntity<String> updateIntFileStatus() {
        int updatedRows = ekartMasterDao.updateIntFileStatus();
        return new ResponseEntity<>("Updated rows: " + updatedRows, HttpStatus.OK);
    }

    @PutMapping("/update-is-processed-client-id")
    public ResponseEntity<String> updateIsProcessedByClientIds(@RequestBody List<String> clientIds) {
        int updatedRows = ekartMasterDao.updateIsProcessedByClientIds(clientIds);
        return new ResponseEntity<>("Updated rows: " + updatedRows, HttpStatus.OK);
    }
}

