/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.requestvalidation;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Constraint {

    private ConstraintType type;

    private String[] parameters;

    private String message;

    public Constraint() {
    }

    public Constraint(Constraint other) {
        this.type = other.type;
        this.parameters = other.parameters;
        this.message = other.message;
    }

    public String[] getParameters() {
        if (parameters == null || parameters.length == 0) {
            return parameters;
        } else {
            return Arrays.stream(parameters).filter(Objects::nonNull).toArray(String[]::new);
        }
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public ConstraintType getType() {
        return type;
    }

    public void setType(ConstraintType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
