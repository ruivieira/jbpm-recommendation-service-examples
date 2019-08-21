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

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.classification.NaiveBayes;
import smile.data.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

public class SmileNaiveBayes extends AbstractPredictionEngine implements PredictionService {

    public static final String IDENTIFIER = "SMILENaiveBayes";

    private static final Logger logger = LoggerFactory.getLogger(SmileNaiveBayes.class);

    private final AttributeDataset dataset;
    private final Map<String, Attribute> smileAttributes;
    protected List<String> attributeNames = new ArrayList<>();
    private final Attribute outcomeAttribute;
    private NaiveBayes model = null;
    private Set<String> outcomeSet = new HashSet<>();
    private final int numAttributes;

    /**
     * Reads the random forest configuration from properties files.
     * "inputs.properties" should contain the input attribute names as keys and attribute types as values.
     *
     * @return A map of input attributes with the attribute name as key and attribute type as value.
     */
    private static NaiveBayesConfiguration readConfigurationFromFile() {

        final NaiveBayesConfiguration configuration = new NaiveBayesConfiguration();

        InputStream inputStream = null;
        final Map<String, AttributeType> inputFeatures = new HashMap<>();
        try {
            Properties prop = new Properties();

            inputStream = SmileNaiveBayes.class.getClassLoader().getResourceAsStream("inputs.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'inputs.properties' in the classpath.");
            }

            for (Object propertyName : prop.keySet()) {
                inputFeatures.put((String) propertyName, AttributeType.valueOf(prop.getProperty((String) propertyName)));
            }

        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        configuration.setInputFeatures(inputFeatures);

        try {
            Properties prop = new Properties();

            inputStream = SmileNaiveBayes.class.getClassLoader().getResourceAsStream("output.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'output.properties' in the classpath.");
            }

            configuration.setOutcomeName(prop.getProperty("name"));
            configuration.setOutcomeType(AttributeType.valueOf(prop.getProperty("type")));
            configuration.setConfidenceThreshold(Double.parseDouble(prop.getProperty("confidence_threshold")));
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        return configuration;
    }


    public SmileNaiveBayes() {
        this(readConfigurationFromFile());
    }

    public SmileNaiveBayes(NaiveBayesConfiguration configuration) {
        this(configuration.getInputFeatures(),
                configuration.getOutcomeName(),
                configuration.getOutcomeType(),
                configuration.getConfidenceThreshold());
    }

    private static Attribute createAttribute(String name, AttributeType type) {
        if (type == AttributeType.NOMINAL) {
            return new NominalAttribute(name);
        } else if (type == AttributeType.NUMERIC) {
            return new NumericAttribute(name);
        } else {
            return new StringAttribute(name);
        }
    }

    public SmileNaiveBayes(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, double confidenceThreshold) {
        super(inputFeatures, outputFeatureName, outputFeatureType, confidenceThreshold);
        smileAttributes = new HashMap<>();
        for (Map.Entry<String, AttributeType> inputFeature : inputFeatures.entrySet()) {
            final String name = inputFeature.getKey();
            final AttributeType type = inputFeature.getValue();
            smileAttributes.put(name, createAttribute(name, type));
            attributeNames.add(name);
        }
        numAttributes = smileAttributes.size();
        outcomeAttribute = createAttribute(outcomeFeatureName, outputFeatureType);
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
            model = new NaiveBayes(NaiveBayes.Model.MULTINOMIAL, outcomeSet.size(), attributeNames.size());

            int[] y = new int[dataset.size()];
            for (int i = 0 ; i < dataset.size() ; i++) {

                y[i] = (int) dataset.get(i).y;
            }

            model.learn(dataset.x(), y);
            final double[] features = buildFeatures(inputData);
            final double[] posteriori = new double[outcomeSet.size()];
            double prediction = model.predict(features, posteriori);

            if (prediction != -1.0) {

                String predictionStr = dataset.responseAttribute().toString(prediction);
                outcomes.put(outcomeAttribute.getName(), predictionStr);
                final double confidence = posteriori[(int) prediction];

                outcomes.put("confidence", confidence);

                logger.debug(inputData + ", prediction = " + predictionStr + ", confidence = " + confidence);

                return new PredictionOutcome(confidence, this.confidenceThreshold, outcomes);
            } else {
                return new PredictionOutcome(0.0, this.confidenceThreshold, outcomes);
            }

        } else {
            return new PredictionOutcome(0.0, this.confidenceThreshold, outcomes);
        }
    }

    /**
     * Train the naive bayes model using data from the human task.
     * @param task Human task data
     * @param inputData A map containing the input attribute names as keys and the attribute values as values.
     * @param outputData A map containing the output attribute names as keys and the attribute values as values.
     */
    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {
        addData(inputData, outputData.get(outcomeAttribute.getName()));
    }
}
