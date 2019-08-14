/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.prediction.naivebayes;

import java.util.*;
import java.util.logging.Logger;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.test.services.AbstractKieServicesTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class NaiveBayesRecommendationServiceProcessTest extends AbstractKieServicesTest {

    private Logger LOGGER = Logger.getLogger(NaiveBayesRecommendationServiceProcessTest.class.getName());

    private List<Long> instances = new ArrayList<>();
    
    @BeforeClass
    public static void setupOnce() {
        System.setProperty("org.jbpm.task.prediction.service", SmileNaiveBayes.IDENTIFIER);
    }
    
    @AfterClass
    public static void cleanOnce() {
        System.clearProperty("org.jbpm.task.prediction.service");
    }
    
    @Override
    protected List<String> getProcessDefinitionFiles() {
        List<String> processes = new ArrayList<String>();
        processes.add("BPMN2-UserTask.bpmn2");
        return processes;
    }

    @Override
    public DeploymentUnit prepareDeploymentUnit() throws Exception {
        // specify GROUP_ID, ARTIFACT_ID, VERSION of your kjar
        return createAndDeployUnit("org.jbpm.test.prediction", "naive-bayes-test", "1.0.0");
    }

    /**
     * shows how after passing min count of 2 input, accuracy goes to > 0.95 (95%) very quickly.
     */
    @Test
    public void test2() {
        Map<String, Object> outputs = new HashMap<>();

        for (int i = 0 ; i < 30; i++) {
            startAndReturnTaskOutputData("test item", "john", 5, false);
            outputs = startAndReturnTaskOutputData("test item", "mary", 5, true);
        }
        assertTrue((double) outputs.get("confidence") > 0.7);
        assertEquals("true", outputs.get("approved"));

    }

    /**
     * This test shows how after passing min count of 2 input with 1 irrelevant param switching between 5 possible
     * values, it takes a while longer to get to high accuracy
     */
    @Test
    public void test3() {
        Map<String, Object> outputs = new HashMap<>();

        for (int i = 0 ; i < 50; i++) {
            startAndReturnTaskOutputData("test item", "john", i % 5, false);
            outputs = startAndReturnTaskOutputData("test item", "mary", i % 5, true);
        }
        assertTrue((double) outputs.get("confidence") > 0.7);
        assertEquals("true", outputs.get("approved"));

    }

    /**
     * This test shows how the accuracy goes extremely quickly to 1.0 (100.0%) if you give it the same result initially
     * it does not even respond to different inputs
     */
    @Test
    public void test4() {
        Map<String, Object> outputs;

        for (int i = 0 ; i < 10; i++) {
            startAndReturnTaskOutputData("test item", "john", i % 5, false);
        }
        startAndReturnTaskOutputData("test item2", "mary", 10, true);
        outputs = startAndReturnTaskOutputData("test item2", "mary", 10, true);

        assertTrue((double) outputs.get("confidence") > 0.7);
        assertEquals("true", outputs.get("approved"));
    }

    /**
     * This test shows how after passing min count of 2 input with 1 irrelevant param switching between 5 possible
     * values, accuracy of completely new input is extremely high.
     */
    @Test
    public void test5() {
        Map<String, Object> outputs;

        for (int i = 0 ; i < 50; i++) {
            startAndReturnTaskOutputData("test item", "john", i % 5, false);
            startAndReturnTaskOutputData("test item", "mary", i % 5, true);
        }
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 10, false);
        startAndReturnTaskOutputData("test item", "john", 5, false);
        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);

        assertTrue((double) outputs.get("confidence") > 0.7);
        assertEquals("true", outputs.get("approved"));
    }

    @Test
    public void test6() {
        Map<String, Object> outputs;

        for (int i = 0 ; i < 100; i++) {
            startAndReturnTaskOutputData("test item", "john", i % 5, !(new Random().nextDouble() < 0.9));
            startAndReturnTaskOutputData("test item", "mary", i % 5, new Random().nextDouble() < 0.9);
            startAndReturnTaskOutputData("test item", "mary", i % 3, new Random().nextDouble() < 0.9);
        }
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 10, false);
        startAndReturnTaskOutputData("test item", "john", 5, false);
        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);

        assertTrue((double) outputs.get("confidence") > 0.5);
        assertEquals("true", outputs.get("approved"));

    }
    
    protected Map<String, Object> startAndReturnTaskOutputData(String item, String userId, Integer level, Boolean approved) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("item", item);
        parameters.put("level", level);
        parameters.put("actor", userId);
        long processInstanceId = processService.startProcess(deploymentUnit.getIdentifier(), "UserTask", parameters);
        instances.add(processInstanceId);
        
        List<TaskSummary> tasks = runtimeDataService.getTasksByStatusByProcessInstanceId(processInstanceId, null, new QueryFilter());
        assertNotNull(tasks);
        
        if (!tasks.isEmpty()) {
        
            Long taskId = tasks.get(0).getId();
            
            Map<String, Object> outputs = userTaskService.getTaskOutputContentByTaskId(taskId);
            assertNotNull(outputs);
            
            userTaskService.completeAutoProgress(taskId, userId, Collections.singletonMap("approved", approved));
            
            return outputs;
        }
        
        return null;
    }
}