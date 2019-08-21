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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PMMLRandomForestBackend extends AbstractPMMLBackend {

    public static final String IDENTIFIER = "PMMLRandomForest";

    private static final Logger logger = LoggerFactory.getLogger(PMMLRandomForestBackend.class);

    /**
     * Reads the random forest configuration from properties files.
     * "inputs.properties" should contain the input attribute names as keys and attribute types as values.
     * @return A map of input attributes with the attribute name as key and attribute type as value.
     */
    private static Map<String, AttributeType> getInputsConfig() {
        InputStream inputStream;
        final Map<String, AttributeType> inputFeaturesConstructor = new HashMap<>();
        try {
            Properties prop = new Properties();

            inputStream = PMMLRandomForestBackend.class.getClassLoader().getResourceAsStream("inputs.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'inputs.properties' in the classpath.");
            }

            for (Object propertyName : prop.keySet()) {
                inputFeaturesConstructor.put((String) propertyName, AttributeType.valueOf(prop.getProperty((String) propertyName)));
            }

        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        return inputFeaturesConstructor;
    }

    private static OutputType getOutputsConfig() {
        InputStream inputStream;
        OutputType outputType = null;
        try {
            Properties prop = new Properties();

            inputStream = PMMLRandomForestBackend.class.getClassLoader().getResourceAsStream("output.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'output.properties' in the classpath.");
            }

            outputType = OutputType.create(prop.getProperty("name"), AttributeType.valueOf(prop.getProperty("type")), Double.parseDouble(prop.getProperty("confidence_threshold")));
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        return outputType;
    }

    private static File getModelFile() {
        InputStream inputStream;
        File modelFile = null;
        try {
            Properties prop = new Properties();

            inputStream = PMMLRandomForestBackend.class.getClassLoader().getResourceAsStream("model.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'model.properties' in the classpath.");
            }

            modelFile = new File(PMMLRandomForestBackend.class.getClassLoader().getResource(prop.getProperty("filename")).getFile());
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        return modelFile;
    }


    public PMMLRandomForestBackend() {
        this(getInputsConfig(), getOutputsConfig(), getModelFile());
    }

    public PMMLRandomForestBackend(Map<String, AttributeType> inputFeatures, OutputType outputType, File pmmlFile) {
        this(inputFeatures, outputType.getName(), outputType.getType(), outputType.getConfidenceThreshold(), pmmlFile);
    }

    public PMMLRandomForestBackend(Map<String, AttributeType> inputFeatures,
                                   String outputFeatureName,
                                   AttributeType outputFeatureType,
                                   double confidenceThreshold,
                                   File pmmlFile) {
        super(inputFeatures, outputFeatureName, outputFeatureType, confidenceThreshold, pmmlFile);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public PredictionOutcome predict(Task task, Map<String, Object> data) {
        Map<String, ?> result = evaluate(data);

        Map<String, Object> outcomes = new HashMap<>();
        String predictionStr;

        Double prediction = (Double) result.get(outcomeFeatureName);
        double confidence = Math.max(Math.abs(0.0 - prediction), Math.abs(1.0 - prediction));
        long predictionInt = Math.round(prediction);

        if (predictionInt == 0) {
            predictionStr = "false";
        } else {
            predictionStr = "true";
        }

        outcomes.put("approved", Boolean.valueOf(predictionStr));
        outcomes.put("confidence", confidence);

        logger.debug(data + ", prediction = " + predictionStr + ", confidence = " + confidence);

        return new PredictionOutcome(confidence, this.confidenceThreshold, outcomes);
    }
}
