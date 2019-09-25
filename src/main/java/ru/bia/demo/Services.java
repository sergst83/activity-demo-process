package ru.bia.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.runtime.connector.Connector;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.bia.demo.client.RestClient;
import ru.bia.process.model.Order;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class Services {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Autowired
    private RestClient restClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @Bean(name = "pickup.sendPickUp")
    Connector sendPickUp() {
        return integrationContext -> {
            try {
                Map<String, Object> response = restClient.createPickUpOperation(integrationContext.getInBoundVariables());

                String status = (String) response.get("status" );
                String orderId = (String) response.get("id" );
                String numberPlate = (String) response.get("numberPlate");
                MutableObject<Date> arriveAt = new MutableObject<>();
                String a = (String) response.get("arriveAt");
                if (a != null) {
                    arriveAt.setValue(new StdDateFormat().parse(a));
                }

                Order order = (Order) integrationContext.getInBoundVariables().get("order");
                order.setId(orderId);

                Map<String, Object> result = new HashMap<>() {{
                    put("status", status);
                    put("order", order);
                    put("numberPlate", numberPlate);
                    put("arriveAt", arriveAt.getValue());
                }};

                integrationContext.addOutBoundVariables(result);
            } catch (Exception  e) {
                logger.error("sendPickUp ", e);
            }

            logVariables(integrationContext);
            return integrationContext;
        };
    }

    @Bean(name = "pickup.checkPickUp")
    Connector checkPickUp() {
        return integrationContext -> {
            try {
                Map<String, Object> response = restClient.checkPickUpOperation(integrationContext.getInBoundVariables());
                String status = (String) response.get("status" );
                String numberPlate = (String) response.get("numberPlate");
                MutableObject<Date> arriveAt = new MutableObject<>();
                MutableObject<Date> pickUpAt = new MutableObject<>();
                String a = (String) response.get("arriveAt");
                if (a != null) {
                    arriveAt.setValue(new StdDateFormat().parse(a));
                }

                String ss = (String) response.get("pickUpAt");
                if (ss != null) {
                    pickUpAt.setValue(new StdDateFormat().parse(ss));
                }

                Map<String, Object> result = new HashMap<>() {{
                    put("status", status);
                    put("arriveAt", arriveAt.getValue());
                    put("numberPlate", numberPlate);
                    put("pickUpAt", pickUpAt.getValue());
                    put("order", integrationContext.getInBoundVariables().get("order"));
                }};

                integrationContext.addOutBoundVariables(result);
            } catch (Exception e) {
                logger.error("checkPickUp ", e);
            }

            logVariables(integrationContext);
            return integrationContext;
        };
    }

    @Bean(name = "putwh.sendPutWh")
    Connector sendPutWh() {
        return integrationContext -> {
            try {
                Map<String, Object> response = restClient.createPutWhOperation(integrationContext.getInBoundVariables());
                String status = (String) response.get("putWhStatus");

                Map<String, Object> result = new HashMap<>() {{
                    put("status", status);
                }};

                integrationContext.addOutBoundVariables(result);
            } catch (Exception e) {
                logger.error("sendPutWh ", e);
            }
            logVariables(integrationContext);
            return integrationContext;
        };
    }

    @Bean(name = "putwh.checkPutWh")
    Connector checkPutWh() {
        return integrationContext -> {
            try {
                Map<String, Object> response = restClient.checkPutWhOperation(integrationContext.getInBoundVariables());
                String status = (String) response.get("putWhStatus");
                MutableObject<Date> puttedAt = new MutableObject<>();
                String a = (String) response.get("puttedAt");
                if (a != null) {
                    puttedAt.setValue(new StdDateFormat().parse(a));
                }

                Map<String, Object> result = new HashMap<>() {{
                    put("status", status);
                    put("puttedAt", puttedAt.getValue());
                }};

                integrationContext.addOutBoundVariables(result);
            } catch (Exception e) {
                logger.error("sendPutWh ", e);
            }
            logVariables(integrationContext);
            return integrationContext;
        };
    }

    private Connector getLogConnector() {
        return integrationContext -> {
            logVariables(integrationContext);
            return integrationContext;
        };
    }

    private void logVariables(IntegrationContext integrationContext) {
        logger.info("==================================");
        Map<String, Object> inBoundVariables = integrationContext.getInBoundVariables();
        Map<String, Object> outBoundVariables = integrationContext.getOutBoundVariables();
        logger.info(">>Connector name: {}", integrationContext.getClientName());
        logger.info(">>inbound: " + inBoundVariables);
        logger.info(">>outbound: " + outBoundVariables);
        logger.info("==================================");
    }
}
