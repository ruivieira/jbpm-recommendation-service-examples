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

/**
 * Encapsulates the model's output information.
 */
public class OutputType {

    private final String name;
    private final AttributeType type;
    private final double confidenceThreshold;

    private OutputType(String name, AttributeType type, double confidenceThreshold) {
        this.name = name;
        this.type = type;
        this.confidenceThreshold = confidenceThreshold;
    }

    /**
     * Create an instance of {@link OutputType}
     * @param name The name of the output attribute
     * @param type The type of the output attribute {@link AttributeType}
     * @return An instance of {@link OutputType}
     */
    public static OutputType create(String name, AttributeType type, double confidenceThreshold) {
        return new OutputType(name, type, confidenceThreshold);
    }

    /**
     * Returns the name of the output attribute
     * @return The name of the output attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the output attribute {@link AttributeType}
     * @return The type of the output attribute
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Returns the confidence threshold to use for automatic task completion
     *
     * @return The confidence threshold, between 0.0 and 1.0
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
}
