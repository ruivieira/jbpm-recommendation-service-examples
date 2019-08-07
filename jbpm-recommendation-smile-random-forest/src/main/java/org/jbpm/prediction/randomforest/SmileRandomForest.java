package org.jbpm.prediction.randomforest;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import smile.classification.RandomForest;
import smile.data.Attribute;
import smile.data.AttributeDataset;
import smile.data.NominalAttribute;

import java.text.ParseException;
import java.util.*;

public class SmileRandomForest extends AbstractPredictionEngine implements PredictionService {

    public static final String IDENTIFIER = "SMILERandomForest";

    private final AttributeDataset dataset;
    private final Map<String, Attribute> smileAttributes;
    protected List<String> attributeNames = new ArrayList<>();
    private final Attribute outcomeAttribute;
    private RandomForest model = null;
    private Set<String> outcomeSet = new HashSet<>();
    private final int numAttributes;

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


    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

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

    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {
        addData(inputData, outputData.get(outcomeAttribute.getName()));
    }
}
