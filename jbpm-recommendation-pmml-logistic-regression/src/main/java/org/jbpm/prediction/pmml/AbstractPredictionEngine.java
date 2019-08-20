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

import java.util.Map;

/**
 * Abstract class for prediction engine needs extended dependent on
 * the model being implemented
 */
abstract public class AbstractPredictionEngine {

    protected Map<String, AttributeType> inputFeatures;
    protected String outcomeFeatureName;
    protected AttributeType outcomeFeatureType;
    protected double confidenceThreshold;

    /**
     * Constructor to initialise a Prediction engine
     * @param inputFeatures
     * @param outputFeatureName
     * @param outputFeatureType
     * @param confidenceThreshold
     */
    public AbstractPredictionEngine(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, double confidenceThreshold) {
        this.inputFeatures = inputFeatures;
        this.outcomeFeatureName = outputFeatureName;
        this.outcomeFeatureType = outputFeatureType;
        this.confidenceThreshold = confidenceThreshold;
    }

}
