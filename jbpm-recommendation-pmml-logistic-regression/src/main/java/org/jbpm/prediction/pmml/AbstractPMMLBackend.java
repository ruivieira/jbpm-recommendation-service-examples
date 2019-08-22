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

public abstract class AbstractPMMLBackend extends AbstractPredictionEngine implements PredictionService {

    private final Evaluator evaluator;
    private final List<? extends InputField> inputFields;
    private final List<? extends TargetField> targetFields;
    protected final List<? extends OutputField> outputFields;

    public AbstractPMMLBackend(Map<String, AttributeType> inputFeatures, String outputFeatureName, double confidenceThreshold, File pmmlFile) {
        super(inputFeatures, outputFeatureName, confidenceThreshold);

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

    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {

    }

    protected abstract Map<String, Object> preProcess(Map<String, Object> data);

    protected Map<String, ?> evaluate(Map<String, Object> data) {
        Map<String, Object> preProcessed = preProcess(data);

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        for(InputField inputField : this.inputFields){

            final FieldName inputName = inputField.getName();

            final Object rawValue = preProcessed.get(inputName.getValue());

            final FieldValue inputValue = inputField.prepare(rawValue);

            arguments.put(inputName, inputValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        return EvaluatorUtil.decodeAll(results);
    }
}
