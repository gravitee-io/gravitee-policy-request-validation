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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.ChainScope;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.Category;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.Policy;
import io.gravitee.policy.api.annotations.Scope;
import io.gravitee.policy.requestvalidation.configuration.RequestValidationPolicyConfiguration;
import io.gravitee.policy.requestvalidation.validator.ExpressionBasedValidator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Policy(
        category = @Category(io.gravitee.policy.api.Category.SECURITY),
        scope = @Scope(ChainScope.API)
)
public class RequestValidationPolicy {

    /**
     * The associated configuration to this Policy
     */
    private RequestValidationPolicyConfiguration configuration;

    private final static String FIELD_MESSAGE = "message";
    private final static String FIELD_CONSTRAINTS = "constraints";
    private final static String DEFAULT_MESSAGE = "Request is not valid according to constraint rules";
    /**
     * Create a new policy instance based on its associated configuration
     *
     * @param configuration the associated configuration to the new policy instance
     */
    public RequestValidationPolicy(RequestValidationPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (configuration.getRules() != null && !configuration.getRules().isEmpty()) {
            Set<ConstraintViolation> violations = validate(executionContext);

            if (violations.isEmpty()) {
                policyChain.doNext(request, response);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode responseNode = mapper.createObjectNode();
                responseNode.put(FIELD_MESSAGE, DEFAULT_MESSAGE);
                ArrayNode constraintsNode = responseNode.putArray(FIELD_CONSTRAINTS);
                violations.forEach(constraintViolation -> constraintsNode.add(constraintViolation.getMessage()));

                String message;

                try {
                    message = mapper.writeValueAsString(responseNode);
                } catch (JsonProcessingException jpe) {
                    message = jpe.getMessage();
                }

                policyChain.failWith(PolicyResult.failure(
                        configuration.getStatus(),
                        message,
                        MediaType.APPLICATION_JSON));
            }
        } else {
            policyChain.doNext(request, response);
        }
    }

    private Set<ConstraintViolation> validate(ExecutionContext executionContext) {
        Set<ConstraintViolation> violations = new HashSet<>();

        for(Rule rule : configuration.getRules()) {
            Validator validator = new ExpressionBasedValidator(executionContext.getTemplateEngine());
            ConstraintViolation constraintViolation = validator.validate(rule.getInput(), rule.getConstraint());
            if (constraintViolation != null) {
                violations.add(constraintViolation);
            }
        }

        return violations;
    }
}
