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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.requestvalidation.configuration.RequestValidationPolicyConfiguration;
import io.gravitee.policy.requestvalidation.validator.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestValidationPolicyTest {

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

    @BeforeEach
    void init() {
        policy = new RequestValidationPolicy(configuration);
        lenient().when(configuration.getStatus()).thenReturn(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    void shouldValidateQueryParameter() {
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
    void shouldNotValidateQueryParameter_notNull() {
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
    void shouldNotValidateQueryParameter_invalidMinConstraint() {
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
        constraint.setParameters(new String[] { "toto" }); // Toto is not a valid number
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    void shouldValidateQueryParameter_multipleRules() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("80"));
        when(request.parameters()).thenReturn(parameters);

        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getAll("my-header")).thenReturn(Collections.singletonList("header-value"));
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
        constraintMin.setParameters(new String[] { "50" });
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
    void shouldValidatePathParameter_el_Rule() {
        // Prepare inbound request
        when(request.pathInfo()).thenReturn("/fr");

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.pathInfos[1]}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        constraint.setParameters(new String[] { "{#request.pathInfos[1]}" });
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Arrays.asList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);
        when(request.pathInfo()).thenReturn("/ro");
        policy.onRequest(request, response, executionContext, policyChain);
        verify(policyChain, times(2)).doNext(request, response);
    }

    @Test
    void shouldValidateQueryParameter_RuleWithParamsContainsNull() {
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
        constraint.setParameters(new String[] { null });
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    Rule prepareRule(String input, String[] params, ConstraintType constraintType) {
        Rule rule = new Rule();
        rule.setInput(input);
        Constraint constraint = new Constraint();
        constraint.setType(constraintType);
        constraint.setParameters(params);
        rule.setConstraint(constraint);
        return rule;
    }

    @Test
    void shouldValidateTheOrderOfViolationsWhenUsingMultipleRules() {
        final String RULE_VALUE = "{#request.headers['my-header']}";
        final String PATTERN = "^[0-9a-fA-F]{32}\\z";

        HttpHeaders headers = mock(HttpHeaders.class);
        when(headers.getAll("my-header")).thenReturn(null);
        when(request.headers()).thenReturn(headers);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        HashMap<Class, String> validatorRegexMap = new HashMap<>();
        validatorRegexMap.put(NotNullConstraintValidator.class, "^'\\w+' can not be null.$");
        validatorRegexMap.put(PatternConstraintValidator.class, "^'.+' is not valid \\(pattern: '.+'\\)$");
        validatorRegexMap.put(MinConstraintValidator.class, "^'.+' must be higher or equals to '\\d+'");
        validatorRegexMap.put(MaxConstraintValidator.class, "^'.+' must be lower or equals to '\\d+'");
        validatorRegexMap.put(
            SizeConstraintValidator.class,
            "'.+' length must be higher or equals to '\\d+' and lower or equals to '\\d+'"
        );
        validatorRegexMap.put(MailConstraintValidator.class, "^.+ is not a valid email.$");

        LinkedHashMap<Rule, ConstraintValidator> rulesMap = new LinkedHashMap<>();
        rulesMap.put(prepareRule(RULE_VALUE, new String[] { "*" }, ConstraintType.NOT_NULL), new NotNullConstraintValidator());
        rulesMap.put(prepareRule(RULE_VALUE, new String[] { PATTERN }, ConstraintType.PATTERN), new PatternConstraintValidator());
        rulesMap.put(prepareRule(RULE_VALUE, new String[] { "5" }, ConstraintType.MIN), new MinConstraintValidator());
        rulesMap.put(prepareRule(RULE_VALUE, new String[] { "5" }, ConstraintType.MAX), new MaxConstraintValidator());
        rulesMap.put(prepareRule(RULE_VALUE, new String[] { "5", "10" }, ConstraintType.SIZE), new SizeConstraintValidator());
        rulesMap.put(prepareRule(RULE_VALUE, new String[] { "wrong-mail" }, ConstraintType.MAIL), new MailConstraintValidator());

        ArrayList<Rule> rules = new ArrayList<>(rulesMap.keySet());
        when(configuration.getRules()).thenReturn(rules);

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain)
            .failWith(
                argThat(result -> {
                    ArrayList<String> violationsTemp = (ArrayList<String>) result.parameters().get("violations");
                    ArrayDeque<String> violations = new ArrayDeque<>(violationsTemp);

                    rulesMap
                        .entrySet()
                        .stream()
                        .forEach(ruleMapEntry -> {
                            ConstraintValidator validator = ruleMapEntry.getValue();
                            String violation = violations.pollFirst();
                            assertThat(Pattern.matches(validatorRegexMap.get(validator.getClass()), violation)).isTrue();
                        });

                    return result.statusCode() == HttpStatusCode.BAD_REQUEST_400;
                })
            );
    }

    @Test
    void shouldValidateQueryParameter_NotRequiredAndNotPresent() {
        // Prepare inbound request
        when(request.parameters()).thenReturn(mock(MultiValueMap.class));

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(false);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    @Test
    void shouldNotValidateQueryParameter_RequiredAndNotPresent() {
        // Prepare inbound request
        when(request.parameters()).thenReturn(mock(MultiValueMap.class));

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(true);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    void shouldValidateQueryParameter_ValidPatternNotRequiredAndPresent() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("myvalue"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(false);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    @Test
    void shouldValidateQueryParameter_ValidPatternRequiredAndPresent() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("myvalue"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(true);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    @Test
    void shouldNotValidateQueryParameter_NotValidPatternNotRequiredAndPresent() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("my1value"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(false);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    void shouldNotValidateQueryParameter_NotValidPatternRequiredAndPresent() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("my1value"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(true);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    void shouldNotValidateQueryParameter_RuleNotRequiredAndEmptyParameter() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList(""));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.PATTERN);
        String[] patterns = { "^[A-Za-z]+$" };
        constraint.setParameters(patterns);
        rule.setConstraint(constraint);
        rule.setIsRequired(false);

        when(configuration.getRules()).thenReturn(Collections.singletonList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }

    @Test
    void shouldValidateQueryParameter_inENUM() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("One"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.ENUM);
        constraint.setParameters(new String[] { "One", "Two" });
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Arrays.asList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).doNext(request, response);
    }

    @Test
    void shouldValidateQueryParameter_outENUM() {
        // Prepare inbound request
        MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
        when(parameters.get("my-param")).thenReturn(Collections.singletonList("Three"));
        when(request.parameters()).thenReturn(parameters);

        // Prepare template engine
        TemplateEngine engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable("request", new EvaluableRequest(request));

        when(executionContext.getTemplateEngine()).thenReturn(engine);

        // Prepare constraint rule
        Rule rule = new Rule();
        rule.setInput("{#request.params['my-param']}");
        Constraint constraint = new Constraint();
        constraint.setType(ConstraintType.ENUM);
        constraint.setParameters(new String[] { "One", "Two" });
        rule.setConstraint(constraint);

        when(configuration.getRules()).thenReturn(Arrays.asList(rule));

        // Execute policy
        policy.onRequest(request, response, executionContext, policyChain);

        // Check results
        verify(policyChain).failWith(argThat(result -> result.statusCode() == HttpStatusCode.BAD_REQUEST_400));
    }
}
