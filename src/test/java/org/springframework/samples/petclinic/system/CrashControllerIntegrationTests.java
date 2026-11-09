/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.system;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration Test for {@link CrashController}. The JSON/HTML error responses are
 * produced by {@link ErrorMapper} (was Spring Boot's {@code BasicErrorController}).
 *
 * @author Alex Lutz
 */
@QuarkusTest
class CrashControllerIntegrationTests {

	@Test
	void triggerExceptionJson() {
		given().accept("application/json")
			.when()
			.get("/oups")
			.then()
			.statusCode(500)
			.body("timestamp", notNullValue())
			.body("status", equalTo(500))
			.body("error", notNullValue())
			.body("message", equalTo("Expected: controller used to showcase what happens when an exception is thrown"))
			.body("path", equalTo("/oups"));
	}

	@Test
	void triggerExceptionHtml() {
		String body = given().accept("text/html")
			.when()
			.get("/oups")
			.then()
			.statusCode(500)
			.extract()
			.asString();
		// html:
		assertThat(body).containsSubsequence("<body>", "<h2>", "Something happened...", "</h2>", "<p>", "Expected:",
				"controller", "used", "to", "showcase", "what", "happens", "when", "an", "exception", "is", "thrown",
				"</p>", "</body>");
		// Not the default error page:
		assertThat(body).doesNotContain("Whitelabel Error Page", "This application has no explicit mapping for");
	}

}
