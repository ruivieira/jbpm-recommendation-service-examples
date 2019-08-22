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

package org.jbpm.prediction.pmml;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

public class PMMLLogisticRegression extends AbstractPMMLBackend {

    public static final String IDENTIFIER = "PMMLLogisticRegression";

    private static final Logger logger = LoggerFactory.getLogger(PMMLLogisticRegression.class);

    public PMMLLogisticRegression() {
        this(readConfigurationFromFile());
    }

    public PMMLLogisticRegression(PMMLLogisticRegressionConfiguration configuration) {
        this(configuration.getInputFeatures(), configuration.getOutcomeName(), configuration.getConfidenceThreshold(), configuration.getModelFile());
    }

    public PMMLLogisticRegression(List<String> inputFeatures, String outputFeatureName, double confidenceThreshold, File pmmlFile) {
        super(inputFeatures, outputFeatureName, confidenceThreshold, pmmlFile);
    }

    /**
     * Reads the PMML model configuration from a properties files.
     * "inputs.properties" should contain the input attribute names as keys and (optional) attribute types as values
     * "output.properties" should contain the output attribute name and the confidence threshold
     * "model.properties" should contain the location of the PMML model
     * @return A map of input attributes with the attribute name as key and attribute type as value
     */
    private static PMMLLogisticRegressionConfiguration readConfigurationFromFile() {

        final PMMLLogisticRegressionConfiguration configuration = new PMMLLogisticRegressionConfiguration();

        InputStream inputStream = null;
        final List<String> inputFeatures = new ArrayList<>();
        try {
            Properties prop = new Properties();

            inputStream = PMMLLogisticRegression.class.getClassLoader().getResourceAsStream("inputs.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'inputs.properties' in the classpath.");
            }

            for (Object propertyName : prop.keySet()) {
                inputFeatures.add((String) propertyName);
            }

        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        configuration.setInputFeatures(inputFeatures);

        try {
            Properties prop = new Properties();

            inputStream = PMMLLogisticRegression.class.getClassLoader().getResourceAsStream("output.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'output.properties' in the classpath.");
            }

            configuration.setOutcomeName(prop.getProperty("name"));
            configuration.setConfidenceThreshold(Double.parseDouble(prop.getProperty("confidence_threshold")));
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }

        File modelFile = null;
        try {
            Properties prop = new Properties();

            inputStream = PMMLLogisticRegression.class.getClassLoader().getResourceAsStream("model.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'model.properties' in the classpath.");
            }

            configuration.setModelFile(new File(PMMLLogisticRegression.class.getClassLoader().getResource(prop.getProperty("filename")).getFile()));
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }

        return configuration;
    }

    /**
     * Returns the processed data (e.g. perform categorisation, etc). If no processing is needed, simply return
     * the original data.
     *
     * @param data A map containing the input data, with attribute names as key and values as values.
     * @return data A map containing the processed data, with attribute names as key and values as values.
     */
    @Override
    protected Map<String, Object> preProcess(Map<String, Object> data) {
        Map<String, Object> preProcessed = new HashMap<>();

        for (String input : data.keySet()) {

            if (input.equals("ActorId")) {
                String strValue = (String) data.get(input);

                int rawValue;

                if (strValue.equals("john")) {
                    rawValue = 0;
                } else {
                    rawValue = 1;
                }

                preProcessed.put(input, rawValue);
            } else {
                preProcessed.put(input, data.get(input));
            }

        }

        return preProcessed;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * Returns a model prediction given the input data
     *
     * @param task      Human task data
     * @param data A map containing the input attribute names as keys and the attribute values as values.
     * @return A {@link PredictionOutcome} containing the model's prediction for the input data.
     */
    @Override
    public PredictionOutcome predict(Task task, Map<String, Object> data) {
        Map<String, ?> result = evaluate(data);

        Map<String, Object> outcomes = new HashMap<>();
        String predictionStr;
        Object predictionValue = result.get(outcomeFeatureName);
        Double confidence;

        if ((Integer) predictionValue == 0) {
            confidence = (Double) result.get("probability_0");
            predictionStr = "false";
        } else {
            confidence = (Double) result.get("probability_1");
            predictionStr = "true";
        }

        outcomes.put("approved", predictionStr);
        outcomes.put("confidence", confidence);

        logger.debug(data + ", prediction = " + predictionStr + ", confidence = " + confidence);

        return new PredictionOutcome(confidence, this.confidenceThreshold, outcomes);
    }
}
