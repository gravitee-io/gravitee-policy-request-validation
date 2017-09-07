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
package io.gravitee.policy.requestvalidation.validator;

import io.gravitee.policy.requestvalidation.Constraint;
import io.gravitee.policy.requestvalidation.ConstraintValidator;
import io.gravitee.policy.requestvalidation.ConstraintViolation;
import io.gravitee.policy.requestvalidation.Validator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultValidator implements Validator {

    @Override
    public ConstraintViolation validate(String input, Constraint constraint) {
        Class<? extends ConstraintValidator> clazz = constraint.getType().validator();
        try {
            ConstraintValidator constraintValidator = clazz.newInstance();
            constraintValidator.initialize(constraint.getParameters());
            boolean valid = constraintValidator.isValid(input);
            if (! valid) {
                ConstraintViolation violation = new ConstraintViolation();
                // Get message from validator
                String[] parameters = (constraint.getParameters() != null) ? constraint.getParameters() : new String[0];
                String[] inputs = new String[parameters.length+1];
                inputs[0] = input;
                System.arraycopy(parameters, 0, inputs, 1, parameters.length);
                String message = (constraint.getMessage() == null || constraint.getMessage().isEmpty())
                        ? constraintValidator.getMessageTemplate() : constraint.getMessage();
                violation.setMessage(String.format(message, inputs));

                return violation;
            }
        } catch (Throwable throwable) {
            // WTD ?
        }

        return null;
    }
}
