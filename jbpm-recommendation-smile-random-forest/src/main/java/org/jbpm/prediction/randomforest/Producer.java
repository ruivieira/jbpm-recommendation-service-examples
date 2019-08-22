package org.jbpm.prediction.randomforest;

import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.services.api.utils.KieServiceConfigurator;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

import java.util.*;

public class Producer {
    protected ProcessService processService;
    protected UserTaskService userTaskService;
    protected RuntimeDataService runtimeDataService;
    protected DeploymentUnit deploymentUnit;


    Producer()
    {

//        KieServiceConfigurator serviceConfigurator = ServiceLoader.load(KieServiceConfigurator.class).iterator().next();
//
//        // build runtime data service
//        runtimeDataService = serviceConfigurator.getRuntimeDataService();
//
//        // build process service
//        processService = serviceConfigurator.getProcessService();
//
//        // build user task service
//        userTaskService = serviceConfigurator.getUserTaskService();
//
//        deploymentUnit = serviceConfigurator.createDeploymentUnit(groupId, artifactid, version);
    }

    /**
     * getInputData the data to be trained
     * @return
     */
    public Map<String, Object> getInputData()
    {
        Map<String, Object> inputs = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            inputs.put("item", "test item");
            inputs.put("level", 5);
            inputs.put("actor", "john");
        }

        return inputs;
    }

    /**
     * add items to the user tasks
     * @param item
     * @param userId
     * @param level
     * @param approved
     * @return
     */
    protected Map<String, Object> getOutputData(String item, String userId, Integer level, Boolean approved) {
        List<Long> instances = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("item", item);
        parameters.put("level", level);
        parameters.put("actor", userId);
        parameters.put("approved",approved);
//        long processInstanceId = processService.startProcess(deploymentUnit.getIdentifier(), "UserTask", parameters);
//        instances.add(processInstanceId);
//
//        List<TaskSummary> tasks = runtimeDataService.getTasksByStatusByProcessInstanceId(processInstanceId, null, new QueryFilter());
//
//        if (!tasks.isEmpty()) {
//
//            Long taskId = tasks.get(0).getId();
//
//            Map<String, Object> outputs = userTaskService.getTaskOutputContentByTaskId(taskId);
//
//            userTaskService.completeAutoProgress(taskId, userId, Collections.singletonMap("approved", approved));
//
//            return outputs;
//        }

        return parameters;
    }
}
