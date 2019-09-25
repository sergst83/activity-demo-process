package ru.bia.demo;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.process.runtime.connector.Connector;
import org.activiti.api.runtime.shared.query.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Configuration
public class Services {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private SecurityUtil securityUtil;

    @Bean(name = "pickup.sendPickUp")
    Connector sendPickUp() {
        return integrationContext -> {
            integrationContext.addOutBoundVariable("status", "running");
            integrationContext.addOutBoundVariable("numberPlate", "12345");
            integrationContext.addOutBoundVariable("order", integrationContext.getInBoundVariables().get("order"));
            integrationContext.addOutBoundVariable("arriveAt", new Date());
            logVariables(integrationContext);
            return integrationContext;
        };
    }

    @Bean(name = "pickup.checkPickUp")
    Connector checkPickUp() {
        return integrationContext -> {
            integrationContext.addOutBoundVariable("status", Math.random() < 0.5 ? "succeeded" : "running");
            integrationContext.addOutBoundVariable("numberPlate", "12345");
            integrationContext.addOutBoundVariable("order", integrationContext.getInBoundVariables().get("order"));
            integrationContext.addOutBoundVariable("arriveAt", new Date());
            integrationContext.addOutBoundVariable("pickUpAt", new Date());
            logVariables(integrationContext);
            return integrationContext;
        };
    }

    @Bean(name = "putwh.sendPutWh")
    Connector sendPutWh() {
        return getLogConnector();
    }

    @Bean(name = "putwh.checkPutWh")
    Connector checkPutWh() {
        return getLogConnector();
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

        try {
            securityUtil.logInAs("system");
            ProcessInstance processInstance = processRuntime.processInstance(integrationContext.getProcessInstanceId());
            listProcessVariables(processInstance);
        } catch (Exception e) {
            logger.error("PeocessInstace log error ", e);
        }
        logger.info("==================================");
    }

    private void listProcessVariables(ProcessInstance processInstance) {
        logger.info(">>> Process instanceId:{}, name:{}", processInstance.getId(), processInstance.getName() );
        logger.info(">>> Process variables:");
        List<VariableInstance> variables = processRuntime.variables(
                ProcessPayloadBuilder
                        .variables()
                        .withProcessInstance(processInstance)
                        .build());
        variables.forEach(variableInstance -> logger.info("\t> " + variableInstance.getName() + " -> " + variableInstance.getValue()));
    }
}
