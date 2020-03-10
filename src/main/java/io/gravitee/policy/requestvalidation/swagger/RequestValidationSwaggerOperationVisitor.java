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
package io.gravitee.policy.requestvalidation.swagger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.policy.api.swagger.v2.SwaggerOperationVisitor;
import io.gravitee.policy.requestvalidation.Constraint;
import io.gravitee.policy.requestvalidation.ConstraintType;
import io.gravitee.policy.requestvalidation.Rule;
import io.gravitee.policy.requestvalidation.configuration.PolicyScope;
import io.gravitee.policy.requestvalidation.configuration.RequestValidationPolicyConfiguration;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestValidationSwaggerOperationVisitor implements SwaggerOperationVisitor {

    private final ObjectMapper mapper  = new ObjectMapper();

    {
        mapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
    }

    @Override
    public Optional<Policy> visit(Swagger descriptor, Operation operation) {
        List<Parameter> parameters = operation.getParameters();

        List<Rule> rules = new ArrayList<>();

        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach(parameter -> {
                String in = parameter.getIn();
                switch (in) {
                    case "query":
                        Rule rule = new Rule();
                        rule.setInput("{#request.params['" + parameter.getName() + "']}");

                        Constraint constraint = new Constraint();
                        constraint.setType(ConstraintType.NOT_NULL);
                        constraint.setMessage(parameter.getName() + " query parameter is required");

                        rule.setConstraint(constraint);

                        rules.add(rule);
                        break;
                    case "header":
                        Rule headerRule = new Rule();
                        headerRule.setInput("{#request.headers['" + parameter.getName() + "'][0]}");

                        Constraint headerConstraint = new Constraint();
                        headerConstraint.setType(ConstraintType.NOT_NULL);
                        headerConstraint.setMessage(parameter.getName() + " header is required");

                        headerRule.setConstraint(headerConstraint);

                        rules.add(headerRule);
                        break;
                }
            });
        }

        if (! rules.isEmpty()) {
            try {
                Policy policy = new Policy();
                policy.setName("policy-request-validation");

                RequestValidationPolicyConfiguration configuration = new RequestValidationPolicyConfiguration();

                configuration.setScope(PolicyScope.REQUEST);
                configuration.setStatus(HttpStatusCode.BAD_REQUEST_400);
                configuration.setRules(rules);

                policy.setConfiguration(mapper.writeValueAsString(configuration));
                return Optional.of(policy);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }
}
