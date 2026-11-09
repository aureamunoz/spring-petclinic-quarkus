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
package org.springframework.samples.petclinic.owner;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test class for {@link VisitController}
 * <p>
 * Migrated from a {@code @WebMvcTest} slice to a {@code @QuarkusTest} with REST Assured.
 *
 * @author Colin But
 * @author Wick Dynex
 */
@QuarkusTest
class VisitControllerTests {

	// Betty Davis and her pet Basil
	private static final int TEST_OWNER_ID = 2;

	private static final int TEST_PET_ID = 2;

	private static final String FORM = "Previous Visits";

	@Test
	void initNewVisitForm() {
		given().when()
			.get("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
			.then()
			.statusCode(200)
			.body(containsString(FORM))
			.body(containsString("Basil"));
	}

	@Test
	void processNewVisitFormSuccess() {
		given().redirects()
			.follow(false)
			.formParam("name", "George")
			.formParam("date", LocalDate.now().plusDays(1).toString())
			.formParam("description", "Visit Description")
			.when()
			.post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void processNewVisitFormHasErrors() {
		given().formParam("name", "George")
			.when()
			.post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
			.then()
			.statusCode(200)
			.body(containsString(FORM))
			.body(containsString("must not be blank"));
	}

	@Test
	void processNewVisitFormHasErrorsWhenVisitDateIsNotInFuture() {
		given().formParam("name", "George")
			.formParam("date", LocalDate.now().toString())
			.formParam("description", "Visit Description")
			.when()
			.post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
			.then()
			.statusCode(200)
			.body(containsString(FORM))
			.body(containsString("Visit date must be in the future"));
	}

	@Test
	void processNewVisitFormHasErrorsWhenDescriptionIsTooLong() {
		given().formParam("name", "George")
			.formParam("date", LocalDate.now().plusDays(1).toString())
			.formParam("description", "a".repeat(256))
			.when()
			.post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
			.then()
			.statusCode(200)
			.body(containsString(FORM))
			.body(containsString("size must be between 0 and 255"));
	}

}
