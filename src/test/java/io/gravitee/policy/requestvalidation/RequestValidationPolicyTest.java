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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.requestvalidation.configuration.RequestValidationPolicyConfiguration;
import io.gravitee.policy.requestvalidation.el.EvaluableRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestValidationPolicyTest {

    private RequestValidationPolicy policy;

    @Mock
    private RequestValidationPolicyConfiguration configuration;

    @Mock
    protected Request request;

    @Mock
    protected Response response;

    @Mock
    protected PolicyChain policyChain;

    @Mock
    protected ExecutionContext executionContext;

    @Before
    public void init() {
        policy = new RequestValidationPolicy(configuration);
        when(configuration.getStatus()).thenReturn(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldValidateQueryParameter() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("my-value"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.NOT_NULL);
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void shouldNotValidateQueryParameter_notNull() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.NOT_NULL);
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    public void shouldNotValidateQueryParameter_invalidMinConstraint() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.MIN);
        constraint.setParameters(new String []{"toto"}); // Toto is not a valid number
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    public void shouldValidateQueryParameter_multipleRules() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("80"));
        when(request.parameters()).thenReturn(parameters);

        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.get("my-header")).thenReturn(Collections.singletonList("header-value"));
        when(request.headers()).thenReturn(headers);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.NOT_NULL);
        Constraint constraintMin = new Constraint();
        constraintMin.setType(ConstraintType.MIN);
        constraintMin.setParameters(new String []{"50"});
        rule.setConstraint(constraint);

        Rule rule2 = new Rule();
        rule2.setInput("{#request.headers['my-header']}");
        Constraint constraint2 = new Constraint();
        constraint2.setType(ConstraintType.NOT_NULL);
        rule2.setConstraint(constraint2);

        when(configuration.getRules()).thenReturn(Arrays.asList(rule, rule2));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    @Test
    public void shouldValidateQueryParameter_RuleWithParamsContainsNull() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("80"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.NOT_NULL);
        constraint.setParameters(new String[]{null});
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }
}
