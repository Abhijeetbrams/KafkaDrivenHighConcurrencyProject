
package com.ecommerce.kafkahighconcurrencyproject.util;
import com.ecommerce.kafkahighconcurrencyproject.constant.AppConstant;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

public class AppUtil {

    private AppUtil() {
        throw new IllegalStateException("AppUtil is a utility class");
    }

    public static RestTemplate getRestTemplate(int readTimeout) {
        return getRestTemplate(readTimeout, AppConstant.HTTP_CONNECT_TIMEOUT);
    }

    public static RestTemplate getRestTemplate(int readTimeout, int connectTimeout) {
        return new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(connectTimeout))
                .setReadTimeout(Duration.ofSeconds(readTimeout)).build();
    }

    public static String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
