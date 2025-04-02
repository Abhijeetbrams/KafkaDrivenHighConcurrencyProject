package com.ecommerce.kafkahighconcurrencyproject.service;

import com.ecommerce.kafkahighconcurrencyproject.dao.EkartMasterDao;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.List;

@Service
public class SQLService {
    private EkartMasterDao ekartMasterDao;

    public void writeTrackingIdsToCsv(PrintWriter writer,List<String> trackingIds) {
        writer.write("tracking_id\n");
        for (String trackingId : trackingIds) {
            writer.write(trackingId + "\n");
        }
    }
}
