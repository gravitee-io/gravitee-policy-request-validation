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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.policy.requestvalidation.configuration.RequestValidationPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@GatewayTest
class RequestValidationPolicyIntegrationTest extends AbstractPolicyTest<RequestValidationPolicy, RequestValidationPolicyConfiguration> {

    @Test
    @DisplayName("Should validate request body")
    @DeployApi("/apis/api-with-a-simple-constraint.json")
    void shouldValidateRequestBody(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String requestBody = new JsonObject().put("firstName", "Gaëtan").toString();

        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(httpClientRequest -> httpClientRequest.rxSend(requestBody))
            .test()
            .await()
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return true;
            })
            .assertNoErrors();

        wiremock.verify(postRequestedFor(urlPathEqualTo("/endpoint")).withRequestBody(equalTo(requestBody)));
    }

    @Test
    @DisplayName("Should return 400 with an error message when request body is invalid")
    @DeployApi("/apis/api-with-a-simple-constraint.json")
    void shouldReturn400WhenRequestBodyIsInvalid(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String requestBody = new JsonObject().put("AnotherKey", "Gaëtan").toString();

        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(httpClientRequest -> httpClientRequest.rxSend(requestBody))
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(400);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(bufferBody -> {
                JsonObject body = new JsonObject(bufferBody.toString());
                assertThat(body.getString("message")).isEqualTo("Request is not valid according to constraint rules");
                assertThat(body.getJsonArray("constraints").size()).isEqualTo(1);
                assertThat(body.getJsonArray("constraints").getString(0)).isEqualTo("Missing firstName");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    @DisplayName("Should return 400 with multiple error messages when multiple constraints are violated")
    @DeployApi("/apis/api-with-multiple-constraints.json")
    void shouldReturn400WhenMultipleConstraintsAreViolated(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String requestBody = new JsonObject()
            .put("AnotherKey", "Gaëtan")
            .put("job", "MANAGER")
            .put("age", 18)
            .put("email", "an Invalid Email")
            .toString();

        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(httpClientRequest -> httpClientRequest.rxSend(requestBody))
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(400);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(bufferBody -> {
                JsonObject body = new JsonObject(bufferBody.toString());
                assertThat(body.getString("message")).isEqualTo("Request is not valid according to constraint rules");
                assertThat(body.getJsonArray("constraints").size()).isEqualTo(4);
                assertThat(body.getJsonArray("constraints").getString(0)).isEqualTo("Missing firstName");
                assertThat(body.getJsonArray("constraints").getString(1)).isEqualTo("Invalid job, must be one of `DEV`, `OPS` or `QA`");
                assertThat(body.getJsonArray("constraints").getString(2)).isEqualTo("Invalid age, must be greater than 20");
                assertThat(body.getJsonArray("constraints").getString(3)).isEqualTo("an Invalid Email is not a valid email.");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    @DisplayName("Should return 400 with an error message when request body is malformed")
    @DeployApi("/apis/api-with-a-simple-constraint.json")
    void shouldReturn400WhenRequestBodyIsMalformed(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String requestBody = "{\n" + "\"field1\": \"value\"\n" + "\"field2\": \"value\"\n" + "}";

        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(httpClientRequest -> httpClientRequest.rxSend(requestBody))
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(400);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(bufferBody -> {
                JsonObject body = new JsonObject(bufferBody.toString());
                assertThat(body.getString("message")).isEqualTo("Request is not valid according to constraint rules");
                assertThat(body.getJsonArray("constraints").size()).isEqualTo(1);
                assertThat(body.getJsonArray("constraints").getString(0))
                    .isEqualTo(
                        "Unable to evaluate expression: {#jsonPath(#request.content, '$.firstName')} -> It might be related to an invalid request body"
                    );
                return true;
            })
            .assertNoErrors();

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
