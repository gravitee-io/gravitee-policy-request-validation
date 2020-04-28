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
import io.gravitee.common.util.Maps;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.requestvalidation.configuration.PolicyScope;
import io.gravitee.policy.requestvalidation.configuration.RequestValidationPolicyConfiguration;
import io.gravitee.policy.requestvalidation.validator.ExpressionBasedValidator;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestValidationPolicy {

    /**
     * The associated configuration to this Policy
     */
    private RequestValidationPolicyConfiguration configuration;

    private final static String FIELD_MESSAGE = "message";
    private final static String FIELD_CONSTRAINTS = "constraints";
    private final static String DEFAULT_MESSAGE = "Request is not valid according to constraint rules";

    private final static String REQUEST_VARIABLE = "request";

    private static final String REQUEST_VALIDATION_INVALID = "REQUEST_VALIDATION_INVALID";


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
        if ((configuration.getScope() == null || configuration.getScope() == PolicyScope.REQUEST)
                && (configuration.getRules() != null && !configuration.getRules().isEmpty())) {
            Set<ConstraintViolation> violations = validate(executionContext);

            if (violations.isEmpty()) {
                policyChain.doNext(request, response);
            } else {
                final List<String> messageViolations = violations.stream().map(ConstraintViolation::getMessage).collect(toList());
                policyChain.failWith(PolicyResult.failure(
                        REQUEST_VALIDATION_INVALID,
                        configuration.getStatus(),
                        createErrorPayload(violations),
                        Maps.<String, Object>builder()
                                .put("violations", messageViolations)
                                .build()));
            }
        } else {
            policyChain.doNext(request, response);
        }
    }

    @OnRequestContent
    public ReadWriteStream onRequestContent(Request request, ExecutionContext executionContext, PolicyChain policyChain) {
        if (configuration.getScope() != null && configuration.getScope() == PolicyScope.REQUEST_CONTENT) {
            return new BufferedReadWriteStream() {

                Buffer buffer = Buffer.buffer();

                @Override
                public SimpleReadWriteStream<Buffer> write(Buffer content) {
                    buffer.appendBuffer(content);
                    return this;
                }

                @Override
                public void end() {
                    String content = buffer.toString();
                    executionContext.getTemplateEngine().getTemplateContext()
                            .setVariable(REQUEST_VARIABLE, new EvaluableRequest(request, content));

                    // Apply validation rules
                    Set<ConstraintViolation> violations = validate(executionContext);

                    if (!violations.isEmpty()) {
                        final List<String> messageViolations = violations.stream().map(ConstraintViolation::getMessage).collect(toList());
                        policyChain.streamFailWith(PolicyResult.failure(
                                REQUEST_VALIDATION_INVALID,
                                configuration.getStatus(),
                                createErrorPayload(violations),
                                Maps.<String, Object>builder()
                                        .put("violations", messageViolations)
                                        .build()));
                    } else {
                        if (buffer.length() > 0) {
                            super.write(buffer);
                        }

                        super.end();
                    }
                }
            };
        }

        return null;
    }

    private Set<ConstraintViolation> validate(ExecutionContext executionContext) {
        LinkedHashSet<ConstraintViolation> violations = new LinkedHashSet<>();
        for (Rule rule : configuration.getRules()) {

            TemplateEngine templateEngine = executionContext.getTemplateEngine();
            String input = templateEngine.getValue(rule.getInput(), String.class);
            if (rule.getIsRequired() || (input != null && !input.isEmpty())) {
                Validator validator = new ExpressionBasedValidator(executionContext.getTemplateEngine());
                ConstraintViolation constraintViolation = validator.validate(rule.getInput(), new Constraint(rule.getConstraint()));
                if (constraintViolation != null) {
                    violations.add(constraintViolation);
                }
            }
        }

        return violations;
    }

    private String createErrorPayload(Set<ConstraintViolation> violations) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put(FIELD_MESSAGE, DEFAULT_MESSAGE);
        ArrayNode constraintsNode = responseNode.putArray(FIELD_CONSTRAINTS);
        violations.forEach(constraintViolation -> constraintsNode.add(constraintViolation.getMessage()));

        try {
            return mapper.writeValueAsString(responseNode);
        } catch (JsonProcessingException jpe) {
            return jpe.getMessage();
        }
    }
}
