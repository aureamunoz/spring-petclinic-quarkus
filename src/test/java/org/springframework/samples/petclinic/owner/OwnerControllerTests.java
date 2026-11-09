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
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test class for {@link OwnerController}
 * <p>
 * Migrated from a {@code @WebMvcTest} slice with a mocked {@code OwnerRepository} to a
 * {@code @QuarkusTest} with REST Assured against the seeded H2 database (Panache Active
 * Record static methods are not mockable with {@code @InjectMock}). Model/view assertions
 * are replaced with assertions on the rendered HTML.
 *
 * @author Colin But
 * @author Wick Dynex
 */
@QuarkusTest
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1; // George Franklin (read-only)

	private static final int OWNER_WITH_VISITS_ID = 6; // Jean Coleman (read-only)

	private static final int UPDATABLE_OWNER_ID = 9; // David Schroeder (modified here)

	@Test
	void initCreationForm() {
		given().when()
			.get("/owners/new")
			.then()
			.statusCode(200)
			.body(containsString("id=\"add-owner-form\""))
			.body(containsString("Add Owner"));
	}

	@Test
	void processCreationFormSuccess() {
		given().redirects()
			.follow(false)
			.formParam("firstName", "Joe")
			.formParam("lastName", "Bloggs")
			.formParam("address", "123 Caramel Street")
			.formParam("city", "London")
			.formParam("telephone", "1316761638")
			.when()
			.post("/owners/new")
			.then()
			.statusCode(303);
	}

	@Test
	void processCreationFormHasErrors() {
		given().formParam("firstName", "Joe")
			.formParam("lastName", "Bloggs")
			.formParam("city", "London")
			.when()
			.post("/owners/new")
			.then()
			.statusCode(200)
			.body(containsString("id=\"add-owner-form\""))
			// address and telephone are missing
			.body(containsString("must not be blank"));
	}

	@Test
	void processCreationFormRejectsCityLongerThanColumn() {
		given().formParam("firstName", "Joe")
			.formParam("lastName", "Bloggs")
			.formParam("address", "123 Caramel Street")
			.formParam("city", "C".repeat(81))
			.formParam("telephone", "1316761638")
			.when()
			.post("/owners/new")
			.then()
			.statusCode(200)
			.body(containsString("id=\"add-owner-form\""))
			.body(containsString("size must be between 0 and 80"));
	}

	@Test
	void processCreationFormRejectsAddressLongerThanColumn() {
		given().formParam("firstName", "Joe")
			.formParam("lastName", "Bloggs")
			.formParam("address", "A".repeat(256))
			.formParam("city", "London")
			.formParam("telephone", "1316761638")
			.when()
			.post("/owners/new")
			.then()
			.statusCode(200)
			.body(containsString("id=\"add-owner-form\""))
			.body(containsString("size must be between 0 and 255"));
	}

	@Test
	void initFindForm() {
		given().when().get("/owners/find").then().statusCode(200).body(containsString("id=\"search-owner-form\""));
	}

	@Test
	void processFindFormSuccess() {
		given().when().get("/owners?page=1").then().statusCode(200).body(containsString("id=\"owners\""));
	}

	@Test
	void processFindFormRedirectsOutOfBoundsPageToFirstPage() {
		for (int page : List.of(0, 2)) {
			given().redirects()
				.follow(false)
				.queryParam("page", page)
				.queryParam("lastName", "Franklin")
				.when()
				.get("/owners")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/owners?page=1&lastName=Franklin"));
		}
	}

	@Test
	void processFindFormByLastName() {
		given().redirects()
			.follow(false)
			.queryParam("page", 1)
			.queryParam("lastName", "Franklin")
			.when()
			.get("/owners")
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + TEST_OWNER_ID));
	}

	@Test
	void processFindFormIgnoresSurroundingWhitespace() {
		for (String lastName : List.of(" Franklin", "Franklin ", " Franklin ")) {
			given().redirects()
				.follow(false)
				.queryParam("page", 1)
				.queryParam("lastName", lastName)
				.when()
				.get("/owners")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/owners/" + TEST_OWNER_ID));
		}
	}

	@Test
	void processFindFormWithWhitespaceOnlyLastNameReturnsAllOwners() {
		given().queryParam("page", 1)
			.queryParam("lastName", "   ")
			.when()
			.get("/owners")
			.then()
			.statusCode(200)
			.body(containsString("id=\"owners\""));
	}

	@Test
	void processFindFormNoOwnersFound() {
		given().queryParam("page", 1)
			.queryParam("lastName", "Unknown Surname")
			.when()
			.get("/owners")
			.then()
			.statusCode(200)
			.body(containsString("id=\"search-owner-form\""))
			.body(containsString("has not been found"));
	}

	@Test
	void initUpdateOwnerForm() {
		given().when()
			.get("/owners/{ownerId}/edit", TEST_OWNER_ID)
			.then()
			.statusCode(200)
			.body(containsString("value=\"Franklin\""))
			.body(containsString("value=\"George\""))
			.body(containsString("value=\"110 W. Liberty St.\""))
			.body(containsString("value=\"Madison\""))
			.body(containsString("value=\"6085551023\""))
			.body(containsString("Update Owner"));
	}

	@Test
	void processUpdateOwnerFormSuccess() {
		given().redirects()
			.follow(false)
			.formParam("firstName", "Joe")
			.formParam("lastName", "Bloggs")
			.formParam("address", "123 Caramel Street")
			.formParam("city", "London")
			.formParam("telephone", "1616291589")
			.when()
			.post("/owners/{ownerId}/edit", UPDATABLE_OWNER_ID)
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + UPDATABLE_OWNER_ID));
	}

	@Test
	void processUpdateOwnerFormUnchangedSuccess() {
		given().redirects()
			.follow(false)
			.contentType("application/x-www-form-urlencoded")
			.when()
			.post("/owners/{ownerId}/edit", UPDATABLE_OWNER_ID)
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + UPDATABLE_OWNER_ID));
	}

	@Test
	void processUpdateOwnerFormHasErrors() {
		given().formParam("firstName", "Joe")
			.formParam("lastName", "Bloggs")
			.formParam("address", "")
			.formParam("telephone", "")
			.when()
			.post("/owners/{ownerId}/edit", TEST_OWNER_ID)
			.then()
			.statusCode(200)
			.body(containsString("id=\"add-owner-form\""))
			.body(containsString("must not be blank"));

		// invalid values must not have been persisted
		given().when().get("/owners/{ownerId}", TEST_OWNER_ID).then().body(containsString("6085551023"));
	}

	@Test
	void showOwner() {
		given().when()
			.get("/owners/{ownerId}", TEST_OWNER_ID)
			.then()
			.statusCode(200)
			.body(containsString("George Franklin"))
			.body(containsString("110 W. Liberty St."))
			.body(containsString("Madison"))
			.body(containsString("6085551023"))
			.body(containsString("Leo"));

		// owner whose pets have visits
		given().when()
			.get("/owners/{ownerId}", OWNER_WITH_VISITS_ID)
			.then()
			.statusCode(200)
			.body(containsString("rabies shot"))
			.body(not(containsString("id=\"success-message\"")));
	}

	@Test
	void showOwnerDisplaysFlashMessage() {
		given().cookie("flash-message", "New+Owner+Created")
			.when()
			.get("/owners/{ownerId}", TEST_OWNER_ID)
			.then()
			.statusCode(200)
			.body(containsString("New Owner Created"));
	}

	// TODO: Migration required — the Spring test injected an Owner with a different id via
	// flashAttr("owner", owner). The migrated form binding never binds the owner id
	// (equivalent of @InitBinder setDisallowedFields("id")), so the mismatch branch in
	// processUpdateOwnerForm cannot be reached through HTTP anymore.
	@Disabled("Owner id is never bound from the request; mismatch cannot be produced over HTTP")
	@Test
	void processUpdateOwnerFormWithIdMismatch() {
	}

}
