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

/**
 * Encapsulates the model's output information.
 */
public class OutputType {

    private final String name;
    private final AttributeType type;

    private OutputType(String name, AttributeType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Create an instance of {@link OutputType}
     * @param name The name of the output attribute
     * @param type The type of the output attribute {@link AttributeType}
     * @return An instance of {@link OutputType}
     */
    public static OutputType create(String name, AttributeType type) {
        return new OutputType(name, type);
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
}
