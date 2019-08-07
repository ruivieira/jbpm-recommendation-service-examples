/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.test.services.AbstractKieServicesTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

import static org.junit.Assert.*;

public class NaiveBayesRecommendationServiceProcessTest extends AbstractKieServicesTest {

    private Logger logger = Logger.getLogger(NaiveBayesRecommendationServiceProcessTest.class.getName());

    private List<Long> instances = new ArrayList<>();
    
    @BeforeClass
    public static void setupOnce() {
        System.setProperty("org.jbpm.task.prediction.service", SmileNaiveBayes.IDENTIFIER);
    }
    
    @AfterClass
    public static void cleanOnce() {
        System.clearProperty("org.jbpm.task.prediction.service");
    }
    
    @After
    public void abortInstances() {
        //instances.forEach(processInstanceId -> processService.abortProcessInstance(processInstanceId));
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
        return createAndDeployUnit("org.jbpm.test.prediction", "random-forest-test", "1.0.0");
    }


    // For this test insert a quantity of true training samples
    // to verify the random forest class/process is functional.
    // Expect confidence > 90.0 and approved to be true.
    @Test
    public void testRepeatedRandomForestPredictionService() {

        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        for (int i = 0 ; i < 20; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
        }

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());

        assertTrue((double) outputs.get("confidence") > 90.0);
        assertEquals(true, outputs.get("approved"));
    }

    // Insert an equal number of true and false samples, making
    // sure the total number of samples is larger than the dataset
    // size threshold (default is 30.) Since the dataset size
    // threshold will have been met and the probability of true
    // and false will be nearly equal, we expect confidence to be
    // lower than 0.55 (55%).
    @Test
    public void testEqualProbabilityRandomForestPredictionService() {

        Map<String, Object> outputs = new HashMap<>();
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();


        for (int i = 0 ; i < 100; i++) {
            startAndReturnTaskOutputData("test item", "john", 5, false);
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
        }

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());

        final double confidence = (double) outputs.get("confidence");
        assertTrue(confidence < 55.0 && confidence > 45.0);
    }

    // Insert a disproportionate partitioning of true and false samples
    // of a sample set larger than the dataset size threshold. In this
    // case true will have higher probability and as such we expect
    // confidence to be high.
    @Test
    public void testUnequalProbabilityRandomForestPredictionService() {

        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();


        outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        for (int i = 0 ; i < 10; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
        }
        for (int i = 0 ; i < 90; i++) {
            Map<String, Object> o = startAndReturnTaskOutputData("test item", "john" , 5, true);
            if (o != null) { // the training hasn't stopped yet
                outputs = o;
            }
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

        }

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());

        assertTrue((double) outputs.get("confidence") > 85.0);
        assertEquals(true, outputs.get("approved"));
    }

    // shows how after testing with a mixed set (3x false, 17x true) how prediction (and probability)
    // evolve when you then keep sending false 
    @Test
    public void test1() {
        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        for (int i = 0 ; i < 17; i++) {
    		outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

        }
    	for (int i = 0 ; i < 40; i++) {
    		outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
        }

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());

    }
    
    // shows how after passing min count of 2 input, accuracy goes to >95% very quickly
    @Test
    public void test2() {
        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        for (int i = 0 ; i < 30; i++) {
    		System.out.print("[" + i + "] ");
    		outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
            outputs = startAndReturnTaskOutputData("test item", "mary", 5, true);
            oob = (Double) outputs.get("oob");
            posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
        }

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());
    }
    
    // shows how after passing min count of 2 input with 1 irrelevant param switching between 5 possible values,
    // it takes a while longer to get to high accuracy
    @Test
    public void test3() {
        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        for (int i = 0 ; i < 50; i++) {
    		System.out.print("[" + i + "] ");
    		outputs = startAndReturnTaskOutputData("test item", "john", i % 5, false);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

            outputs = startAndReturnTaskOutputData("test item", "mary", i % 5, true);
            oob = (Double) outputs.get("oob");
            posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);
        }

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());
    }
    
    // shows how the accuracy goes extremely quickly to 100.0% if you give it the same result initially
    // it does not even respond to different inputs
    @Test
    public void test4() {
        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        for (int i = 0 ; i < 10; i++) {
    		System.out.print("[" + i + "] ");
    		outputs = startAndReturnTaskOutputData("test item", "john", i % 5, false);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

        }
		outputs = startAndReturnTaskOutputData("test item2", "mary", 10, true);
        Double oob = (Double) outputs.get("oob");
        Double posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "mary", 10, true);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());
    }
    
    // shows how after passing min count of 2 input with 1 irrelevant param switching between 5 possible values,
    // accuracy of completely new input is extremely high, why?
    @Test
    public void test5() {
        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        for (int i = 0 ; i < 50; i++) {
    		System.out.print("[" + i + "] ");
    		outputs = startAndReturnTaskOutputData("test item", "john", i % 5, false);
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

            outputs = startAndReturnTaskOutputData("test item", "mary", i % 5, true);
            oob = (Double) outputs.get("oob");
            posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);


        }
		outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        Double oob = (Double) outputs.get("oob");
        Double posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());
    }

    @Test
    public void test6() {
        Map<String, Object> outputs;
        List<Double> oobError = new ArrayList<>();
        List<Double> posteriors = new ArrayList<>();

        for (int i = 0 ; i < 100; i++) {
            System.out.print("[" + i + "] ");
            outputs = startAndReturnTaskOutputData("test item", "john", i % 5, !(new Random().nextDouble() < 0.9));
            Double oob = (Double) outputs.get("oob");
            Double posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

            outputs = startAndReturnTaskOutputData("test item", "mary", i % 5, new Random().nextDouble() < 0.9);
            oob = (Double) outputs.get("oob");
            posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

            outputs = startAndReturnTaskOutputData("test item", "mary", i % 3, new Random().nextDouble() < 0.9);
            oob = (Double) outputs.get("oob");
            posterior = (Double) outputs.get("confidence");
            if (oob!=null) oobError.add(oob);
            if (posterior!=null) posteriors.add(posterior);

        }
        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        Double oob = (Double) outputs.get("oob");
        Double posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        oob = (Double) outputs.get("oob");
        posterior = (Double) outputs.get("confidence");
        if (oob!=null) oobError.add(oob);
        if (posterior!=null) posteriors.add(posterior);

        final List<Integer> x = IntStream.range(0, oobError.size()).boxed().collect(Collectors.toList());
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
