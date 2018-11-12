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

import io.gravitee.gateway.api.expression.TemplateEngine;
import io.gravitee.policy.requestvalidation.Constraint;
import io.gravitee.policy.requestvalidation.ConstraintViolation;

import java.util.Arrays;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExpressionBasedValidator extends DefaultValidator {

    private final TemplateEngine templateEngine;

    public ExpressionBasedValidator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public ConstraintViolation validate(String input, Constraint constraint) {
        if (constraint.getParameters() != null) {
            constraint.setParameters(Arrays
                    .stream(constraint.getParameters())
                    .map(templateEngine::convert)
                    .toArray(String[]::new));
        }

        return super.validate(templateEngine.convert(input), constraint);
    }
}
