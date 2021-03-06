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

import org.kie.internal.task.api.prediction.PredictionService;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;


public class PMMLRandomForestRegistry {
    private static final ServiceLoader<PredictionService> foundServices = ServiceLoader.load(PredictionService.class, PMMLRandomForestRegistry.class.getClassLoader());
    private String selectedService = System.getProperty("org.jbpm.prediction.pmml", PMMLRandomForestBackend.IDENTIFIER);
    private Map<String, PredictionService> predictionServices = new HashMap<>();

    private PMMLRandomForestRegistry() {

        foundServices
                .forEach(strategy -> predictionServices.put(strategy.getIdentifier(), strategy));
    }
    public static PMMLRandomForestRegistry get() {
        return Holder.INSTANCE;
    }

    public PredictionService getService() {
        PredictionService predictionService = predictionServices.get(selectedService);
        if (predictionService == null) {
            throw new IllegalArgumentException("No prediction service was found with id " + selectedService);
        }

        return predictionService;
    }

    private static class Holder {
        static final PMMLRandomForestRegistry INSTANCE = new PMMLRandomForestRegistry();
    }

    public synchronized void addStrategy(PMMLRandomForestBackend predictionService) {
        this.predictionServices.put(predictionService.getIdentifier(), predictionService);

    }
}
