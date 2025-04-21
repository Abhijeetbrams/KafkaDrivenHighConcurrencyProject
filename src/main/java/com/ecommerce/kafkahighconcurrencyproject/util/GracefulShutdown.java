
package com.ecommerce.kafkahighconcurrencyproject.util;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import lombok.extern.log4j.Log4j2;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Component
public class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

    @Autowired
    ConfigProperty configProperty;

    private AtomicReference<Connector> connector = new AtomicReference<>();

    private AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @Override
    public void customize(Connector connector) {
        this.connector.set(connector);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            int timer = configProperty.getEnvironment().equals("dev") ? 0 : 30;
            log.info("Starting GracefulShutdown.");
            shuttingDown.set(true);
            Thread.sleep(timer * 1000L);
            this.connector.get().pause();
            shutdownThreadPoolExecutor(timer);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Exception GracefulShutdown {}", ex);
        }
    }

    private void shutdownThreadPoolExecutor(int timer) throws InterruptedException {
        Executor executor = this.connector.get().getProtocolHandler().getExecutor();
        if (executor instanceof ThreadPoolExecutor) {
            log.info(executor);
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
            threadPoolExecutor.shutdown();
            if (!threadPoolExecutor.awaitTermination(timer, TimeUnit.SECONDS)) {
                log.warn("Tomcat thread pool did not shut down gracefully within "
                        + "30 seconds. Proceeding with forceful shutdown");
            }
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
