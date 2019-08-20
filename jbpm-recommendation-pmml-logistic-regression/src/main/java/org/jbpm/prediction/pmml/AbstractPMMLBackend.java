package org.jbpm.prediction.pmml;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.*;
import org.jpmml.evaluator.visitors.DefaultVisitorBattery;
import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionService;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract class for PMML backend so any implementation of a PMML model will extend this
 */
public abstract class AbstractPMMLBackend extends AbstractPredictionEngine implements PredictionService {

    private final Evaluator evaluator;
    private final List<? extends InputField> inputFields;
    private final List<? extends TargetField> targetFields;
    protected final List<? extends OutputField> outputFields;

    /**
     *
     * @param inputFeatures
     * @param outputFeatureName
     * @param outputFeatureType
     * @param confidenceThreshold
     * @param pmmlFile
     */
    public AbstractPMMLBackend(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, double confidenceThreshold, File pmmlFile) {
        super(inputFeatures, outputFeatureName, outputFeatureType, confidenceThreshold);

        Evaluator _evalutator = null;
        try {
            _evalutator = new LoadingModelEvaluatorBuilder()
                    .setLocatable(false)
                    .setVisitors(new DefaultVisitorBattery())
                    .load(pmmlFile)
                    .build();
            _evalutator.verify();
        } catch (IOException | SAXException | JAXBException e) {
            e.printStackTrace();
        } finally {
            this.evaluator = _evalutator;
        }

        this.inputFields = evaluator.getInputFields();
        this.targetFields = evaluator.getTargetFields();
        this.outputFields = evaluator.getOutputFields();
    }

    /**
     * Train a model
     * @param task
     * @param inputData
     * @param outputData
     */
    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {

    }

    /**
     * Evaluate the accuracy of the model
     * @param data
     * @return
     */
    protected Map<String, ?> evaluate(Map<String, Object> data) {
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        for(InputField inputField : this.inputFields){

            FieldName inputName = inputField.getName();
            Object rawValue;
            if (inputName.getValue().equals("ActorId")) {

                String strValue = (String) data.get(inputName.getValue());

                if (strValue.equals("john")) {
                    rawValue = 0;
                } else {
                    rawValue = 1;
                }

            } else {
                rawValue = data.get(inputName.getValue());
            }

            // Transforming an arbitrary user-supplied value to a known-good PMML value
            FieldValue inputValue = inputField.prepare(rawValue);

            arguments.put(inputName, inputValue);
        }

        // Evaluating the model with known-good arguments
        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        return EvaluatorUtil.decodeAll(results);
    }
}
