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

package org.jbpm.prediction.randomforest;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import smile.classification.RandomForest;
import smile.data.Attribute;
import smile.data.AttributeDataset;
import smile.data.NominalAttribute;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

public class SmileRandomForest extends AbstractPredictionEngine implements PredictionService {

    private Logger logger = Logger.getLogger(SmileRandomForest.class.getName());

    public static final String IDENTIFIER = "SMILERandomForest";

    private final AttributeDataset dataset;
    private final Map<String, Attribute> smileAttributes;
    protected List<String> attributeNames = new ArrayList<>();
    private final Attribute outcomeAttribute;
    private RandomForest model = null;
    private Set<String> outcomeSet = new HashSet<>();
    private final int numAttributes;

    /**
     * Reads the random forest configuration from properties files.
     * "inputs.properties" should contain the input attribute names as keys and attribute types as values.
     * @return A map of input attributes with the attribute name as key and attribute type as value.
     */
    private static Map<String, AttributeType> getInputsConfig() {
        InputStream inputStream = null;
        final Map<String, AttributeType> inputFeaturesConstructor = new HashMap<>();
        try {
            Properties prop = new Properties();

            inputStream = SmileRandomForest.class.getClassLoader().getResourceAsStream("inputs.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'inputs.properties' in the classpath.");
            }

            for (Object propertyName : prop.keySet()) {
                inputFeaturesConstructor.put((String) propertyName, AttributeType.valueOf(prop.getProperty((String) propertyName)));
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return inputFeaturesConstructor;
    }

    private static OutputType getOutputsConfig() {
        InputStream inputStream;
        OutputType outputType = null;
        try {
            Properties prop = new Properties();

            inputStream = SmileRandomForest.class.getClassLoader().getResourceAsStream("output.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'output.properties' in the classpath.");
            }

            outputType = OutputType.create(prop.getProperty("name"), AttributeType.valueOf(prop.getProperty("type")));
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return outputType;
    }

    public SmileRandomForest() {
        this(getInputsConfig(), getOutputsConfig());
    }

    public SmileRandomForest(Map<String, AttributeType> inputFeatures, OutputType outputType) {
        this(inputFeatures, outputType.getName(), outputType.getType());
    }

    public SmileRandomForest(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType) {
        super(inputFeatures, outputFeatureName, outputFeatureType);
        smileAttributes = new HashMap<>();
        for (Map.Entry<String, AttributeType> inputFeature : inputFeatures.entrySet()) {
            final String name = inputFeature.getKey();
            final AttributeType type = inputFeature.getValue();
            if (type == AttributeType.NOMINAL) {
                smileAttributes.put(name, new NominalAttribute(name));
                attributeNames.add(name);
            }
        }
        numAttributes = smileAttributes.size();

        if (outputFeatureType == AttributeType.NOMINAL) {
            outcomeAttribute = new NominalAttribute(outputFeatureName);
        } else {
            // only dealing with nominal features at the moment
            outcomeAttribute = new NominalAttribute(outputFeatureName);
        }

        dataset = new AttributeDataset("dataset", smileAttributes.values().toArray(new Attribute[numAttributes]), outcomeAttribute);
    }

    /**
     * Add the data provided as a map to a Smile {@link smile.data.Dataset}.
     * @param data A map containing the input attribute names as keys and the attribute values as values.
     * @param outcome The value of the outcome (output data).
     */
    public void addData(Map<String, Object> data, Object outcome) {
        final double[] features = new double[numAttributes];
        int i = 0;
        for (String attrName : smileAttributes.keySet()) {
            try {
                features[i] = smileAttributes.get(attrName).valueOf(data.get(attrName).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            i++;
        }
        try {
            final String outcomeStr = outcome.toString();
            outcomeSet.add(outcomeStr);
            dataset.add(features, outcomeAttribute.valueOf(outcomeStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Build a set of features compatible with Smile's datasets from the map of input data
     * @param data A map containing the input attribute names as keys and the attribute values as values.
     * @return A feature vector as a array of doubles.
     */
    protected double[] buildFeatures(Map<String, Object> data) {
        final double[] features = new double[numAttributes];
        for (int i = 0 ; i < numAttributes ; i++) {
            final String attrName = attributeNames.get(i);
            try {
                features[i] = smileAttributes.get(attrName).valueOf(data.get(attrName).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return features;
    }

    /**
     * Returns the service's identifier
     * @return The service identifier
     */
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * Returns a model prediction given the input data
     * @param task Human task data
     * @param inputData A map containing the input attribute names as keys and the attribute values as values.
     * @return A {@link PredictionOutcome} containing the model's prediction for the input data.
     */
    @Override
    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {
        Map<String, Object> outcomes = new HashMap<>();
        if (outcomeSet.size() >= 2) {
            model = new RandomForest(dataset, 100);
            final double[] features = buildFeatures(inputData);
            final double[] posteriori = new double[outcomeSet.size()];
            double prediction = model.predict(features, posteriori);

            String predictionStr = dataset.responseAttribute().toString(prediction);
            outcomes.put(outcomeAttribute.getName(), predictionStr);
            final double confidence = posteriori[(int) prediction];
            outcomes.put("confidence", confidence);

            System.out.println(inputData + ", prediction = " + predictionStr + ", confidence = " + confidence);

            return new PredictionOutcome(confidence, 100, outcomes);
        } else {
            return new PredictionOutcome(0.0, 100.0, outcomes);
        }
    }

    /**
     * Train the random forest model using data from the human task.
     * @param task Human task data
     * @param inputData A map containing the input attribute names as keys and the attribute values as values.
     * @param outputData A map containing the output attribute names as keys and the attribute values as values.
     */
    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {
        addData(inputData, outputData.get(outcomeAttribute.getName()));
    }
}
