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

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.*;
import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionService;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPMMLBackend implements PredictionService {

    private final Evaluator evaluator;
    private final List<? extends InputField> inputFields;
    private final List<? extends TargetField> targetFields;
    protected final List<? extends OutputField> outputFields;

    protected List<String> inputFeatures;
    protected String outcomeFeatureName;
    protected double confidenceThreshold;


    public AbstractPMMLBackend(List<String> inputFeatures, String outputFeatureName, double confidenceThreshold, File pmmlFile) {
        this.inputFeatures = inputFeatures;
        this.outcomeFeatureName = outputFeatureName;
        this.confidenceThreshold = confidenceThreshold;


        Evaluator _evalutator = null;
        try {
            _evalutator = new LoadingModelEvaluatorBuilder()
                    .setLocatable(false)
                    .setVisitors(new DefaultVisitorBattery())
                    .load(pmmlFile)
                    .build();
            _evalutator.verify();

            this.evaluator = _evalutator;

            this.inputFields = this.evaluator.getInputFields();
            this.targetFields = evaluator.getTargetFields();
            this.outputFields = evaluator.getOutputFields();

        } catch (IOException | SAXException | JAXBException e) {
            throw new RuntimeException("Could not initialise model");
        }
    }

    /**
     * Method to train a model. In the PMML case, this is a no-op.
     *
     * @param task       Human task data
     * @param inputData  A map containing the input attribute names as keys and the attribute values as values.
     * @param outputData A map containing the output attribute names as keys and the attribute values as values.
     */
    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {

    }

    /**
     * Returns the processed data (e.g. perform categorisation, etc). If no processing is needed, simply return
     * the original data.
     *
     * @param data A map containing the input data, with attribute names as key and values as values.
     * @return data A map containing the processed data, with attribute names as key and values as values.
     */
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
