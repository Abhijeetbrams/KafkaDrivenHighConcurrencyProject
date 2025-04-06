/**
 * ****************************************************************************
 *
 * Copyright (c) 2021, FarEye and/or its affiliates. All rights
 * reserved.
 * ___________________________________________________________________________________
 *
 *
 * NOTICE: All information contained herein is, and remains the property of
 * FaEye and its suppliers,if any. The intellectual and technical concepts
 * contained herein are proprietary to FarEye. and its suppliers and
 * may be covered by us and Foreign Patents, patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from FarEye
 */

package com.ecommerce.kafkahighconcurrencyproject.util;

import co.fareye.config.ConfigProperty;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Log4j2
public class EmailService {

    @Autowired
    private ConfigProperty configProperty;

    @Autowired
    private KafkaTemplate<String, Object> template;

    public void sendEmail(EmailDto emailDto) {
        try {
            // if body exists add to props
            if (!StringUtils.isEmpty(emailDto.getBody())) {
                Map<String, Serializable> props = new HashMap<>();
                props.put("info", emailDto.getBody());
                emailDto.setProps(props);
            }

            this.template.send(configProperty.getEmailTopic(), UUID.randomUUID().toString(), emailDto);
        } catch (Exception e) {
            log.error("Exception sendEmail {}", e);
        }
    }
}
