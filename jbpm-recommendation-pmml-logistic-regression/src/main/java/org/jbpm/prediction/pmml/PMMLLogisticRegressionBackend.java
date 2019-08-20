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

/**
 * Implementation of the AbstractPMML backend for a logisitc regression model
 */
public class PMMLLogisticRegressionBackend extends AbstractPMMLBackend {

    public static final String IDENTIFIER = "PMMLLogisticRegression";

    private static final Logger logger = LoggerFactory.getLogger(PMMLLogisticRegressionBackend.class);

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

            inputStream = PMMLLogisticRegressionBackend.class.getClassLoader().getResourceAsStream("inputs.properties");

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

            inputStream = PMMLLogisticRegressionBackend.class.getClassLoader().getResourceAsStream("output.properties");
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

            inputStream = PMMLLogisticRegressionBackend.class.getClassLoader().getResourceAsStream("model.properties");

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Could not find the property file 'model.properties' in the classpath.");
            }

            modelFile = new File(PMMLLogisticRegressionBackend.class.getClassLoader().getResource(prop.getProperty("filename")).getFile());
        } catch (Exception e) {
            logger.error("Exception: " + e);
        }
        return modelFile;
    }


    public PMMLLogisticRegressionBackend() {
        this(getInputsConfig(), getOutputsConfig(), getModelFile());
    }

    public PMMLLogisticRegressionBackend(Map<String, AttributeType> inputFeatures, OutputType outputType, File pmmlFile) {
        this(inputFeatures, outputType.getName(), outputType.getType(), outputType.getConfidenceThreshold(), pmmlFile);
    }

    public PMMLLogisticRegressionBackend(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, double confidenceThreshold, File pmmlFile) {
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

        return new PredictionOutcome(confidence, 100, outcomes);
    }
}
