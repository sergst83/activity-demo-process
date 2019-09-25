package ru.bia.demo;

import org.activiti.api.model.shared.event.VariableCreatedEvent;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.process.runtime.events.ProcessCompletedEvent;
import org.activiti.api.process.runtime.events.ProcessCreatedEvent;
import org.activiti.api.process.runtime.events.ProcessStartedEvent;
import org.activiti.api.process.runtime.events.ProcessUpdatedEvent;
import org.activiti.api.process.runtime.events.listener.ProcessRuntimeEventListener;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.parameters.P;
import ru.bia.process.model.Cargo;
import ru.bia.process.model.Characteristic;
import ru.bia.process.model.Order;
import ru.bia.process.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

	private Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	@Autowired
	private ProcessRuntime processRuntime;

	@Autowired
	private TaskRuntime taskRuntime;

	@Autowired
	private SecurityUtil securityUtil;

	private List<VariableCreatedEvent> variableCreatedEvents = new ArrayList<>();

	private List<ProcessCompletedEvent> processCompletedEvents = new ArrayList<>();

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void run(String... args) {
		securityUtil.logInAs("system");

		listAvailableProcesses();
		ProcessInstance processInstance = startProcess();
		listProcessVariables(processInstance);
		completeAvailableTasks();
		listAllCreatedVariables();
	}

	private void listAvailableProcesses() {
		Page<ProcessDefinition> processDefinitionPage = processRuntime.processDefinitions(Pageable.of(0,
				10));
		logger.info("> Available Process definitions: " + processDefinitionPage.getTotalItems());
		for (ProcessDefinition pd : processDefinitionPage.getContent()) {
			logger.info("\t > Process definition: " + pd);
		}
	}

	private ProcessInstance startProcess() {
        return processRuntime.start(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("process-1d706ba2-b04a-49d8-a4b5-08f66c9a14a3")
                .withName("FullProcess")
                .withVariable("inOrder", getOrder())
                .withBusinessKey("demo-test")
                .build());
	}

	private Order getOrder() {
		Order order = new Order();
		Product product = new Product();
		product.setIdSpec(UUID.randomUUID().toString());
		Characteristic characteristic = new Characteristic();
		characteristic.setAddressFrom("Адрес отправления");
		characteristic.setAddressTo("Адрес назначения");
		Cargo cargo = new Cargo();
		cargo.setHeight(1);
		cargo.setLength(1);
		cargo.setPackageType("коробка");
		cargo.setWeight(1);
		cargo.setWidth(1);
		characteristic.setCargo(cargo);
		product.setCaracteristics(Collections.singletonList(characteristic));
		order.setProducts(Collections.singletonList(product));
		return order;
	}

	private void listProcessVariables(ProcessInstance processInstance) {
		logger.info(">>> Process variables:");
		List<VariableInstance> variables = processRuntime.variables(
				ProcessPayloadBuilder
						.variables()
						.withProcessInstance(processInstance)
						.build());
		variables.forEach(variableInstance -> logger.info("\t> " + variableInstance.getName() + " -> " + variableInstance.getValue()));
	}

	private void completeAvailableTasks() {
		Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
				20));
		tasks.getContent().forEach(task -> {
			logger.info(">>> Performing task -> " + task);
			listTaskVariables(task);
			taskRuntime.complete(TaskPayloadBuilder
					.complete()
					.withTaskId(task.getId())
					.withVariable("rating", 5)
					.build());
		});
	}

	private void listTaskVariables(Task task) {
		logger.info(">>> Task variables:");
		taskRuntime.variables(TaskPayloadBuilder
				.variables()
				.withTaskId(task.getId())
				.build())
				.forEach(variableInstance -> logger.info("\t> " + variableInstance.getName() + " -> " + variableInstance.getValue()));
	}

	private void listAllCreatedVariables() {
		logger.info(">>> All created variables:");
		variableCreatedEvents.forEach(variableCreatedEvent -> logger.info("\t> name:`" + variableCreatedEvent.getEntity().getName()
				+ "`, value: `" + variableCreatedEvent.getEntity().getValue()
				+ "`, processInstanceId: `" + variableCreatedEvent.getEntity().getProcessInstanceId()
				+ "`, taskId: `" + variableCreatedEvent.getEntity().getTaskId() + "`"));
	}

	private void listCompletedProcesses() {
		logger.info(">>> Completed process Instances: ");
		processCompletedEvents.forEach(processCompletedEvent -> logger.info("\t> Process instance : " + processCompletedEvent.getEntity()));
	}

	@Bean
	public ProcessRuntimeEventListener<ProcessCompletedEvent> processCompletedListener() {
		return processEvent -> logger.info(">>> Process Completed: '"
				+ processEvent.getEntity().getName() +
				"' We can send a notification to the initiator: " + processEvent.getEntity().getInitiator());
	}

	@Bean
    public ProcessRuntimeEventListener<ProcessUpdatedEvent> processUpdatedListener() {
	    return processEvent -> listProcessVariables(processEvent.getEntity());
    }

    @Bean
    public ProcessRuntimeEventListener<ProcessStartedEvent> processCreatedListiner() {
	    return processEvent -> {
	    	logger.info(">>> Created Process Instance: " + processEvent.getEntity());
			securityUtil.logInAs("system");
			listProcessVariables(processEvent.getEntity());
		};
    }
}
