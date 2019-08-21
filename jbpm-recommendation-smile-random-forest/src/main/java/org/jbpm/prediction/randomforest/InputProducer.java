package org.jbpm.prediction.randomforest;

import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.DeploymentUnit;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

import java.util.*;

public class InputProducer {
    protected ProcessService processService;
    protected UserTaskService userTaskService;
    protected RuntimeDataService runtimeDataService;
    protected DeploymentUnit deploymentUnit;


    InputProducer()
    {
        build();
        //TODO add this is to train model
    }

    /**
     * build the data to be trained
     * @return
     */
    public Map<String, Object> build()
    {
        Map<String, Object> outputs;
        outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        for (int i = 0; i < 20; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        }
        return outputs;
    }

    /**
     * add items to the user tasks
     * @param item
     * @param userId
     * @param level
     * @param approved
     * @return
     */
    protected Map<String, Object> startAndReturnTaskOutputData(String item, String userId, Integer level, Boolean approved) {
        List<Long> instances = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("item", item);
        parameters.put("level", level);
        parameters.put("actor", userId);
        long processInstanceId = processService.startProcess(deploymentUnit.getIdentifier(), "UserTask", parameters);
        instances.add(processInstanceId);

        List<TaskSummary> tasks = runtimeDataService.getTasksByStatusByProcessInstanceId(processInstanceId, null, new QueryFilter());

        if (!tasks.isEmpty()) {

            Long taskId = tasks.get(0).getId();

            Map<String, Object> outputs = userTaskService.getTaskOutputContentByTaskId(taskId);

            userTaskService.completeAutoProgress(taskId, userId, Collections.singletonMap("approved", approved));

            return outputs;
        }

        return null;
    }
}
