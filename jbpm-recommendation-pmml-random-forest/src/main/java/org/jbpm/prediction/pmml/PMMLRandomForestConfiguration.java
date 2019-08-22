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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the model's output information.
 */
public class PMMLRandomForestConfiguration {

    private String outcomeName;
    private double confidenceThreshold;
    private List<String> inputFeatures = new ArrayList<>();
    private File modelFile;

    public File getModelFile() {
        return modelFile;
    }

    public void setModelFile(File modelFile) {
        this.modelFile = modelFile;
    }

    /**
     * Returns the name of the output attribute
     *
     * @return The name of the output attribute
     */
    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    /**
     * Returns the confidence threshold to use for automatic task completion
     *
     * @return The confidence threshold, between 0.0 and 1.0
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public List<String> getInputFeatures() {
        return inputFeatures;
    }

    public void setInputFeatures(List<String> inputFeatures) {
        this.inputFeatures = inputFeatures;
    }
}
