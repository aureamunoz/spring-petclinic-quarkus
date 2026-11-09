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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

import java.time.LocalDate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.PersistenceException;

/**
 * Test class for the {@link PetController}
 * <p>
 * Migrated from a {@code @WebMvcTest} slice with mocked repositories to a
 * {@code @QuarkusTest} with REST Assured against the seeded H2 database.
 *
 * @author Colin But
 * @author Wick Dynex
 */
@QuarkusTest
class PetControllerTests {

	// Eduardo Rodriquez: pets Rosy (id 3) and Jewel (id 4)
	private static final int TEST_OWNER_ID = 3;

	private static final int TEST_PET_ID = 3;

	// Peter McTavish: pet George (id 6), Jeff Black: pet Lucky (id 9)
	private static final int SAME_NAME_OWNER_ID = 5;

	private static final int SAME_NAME_PET_ID = 6;

	private static final int UPDATABLE_OWNER_ID = 7;

	private static final int UPDATABLE_PET_ID = 9;

	private static final String FORM = "id=\"name\"";

	@Test
	void initCreationForm() {
		given().when()
			.get("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
			.then()
			.statusCode(200)
			.body(containsString(FORM))
			.body(containsString("Add Pet"));
	}

	@Test
	void processCreationFormSuccess() {
		given().redirects()
			.follow(false)
			.formParam("name", "Betty")
			.formParam("type", "hamster")
			.formParam("birthDate", "2015-02-12")
			.when()
			.post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + TEST_OWNER_ID));
	}

	@Nested
	class ProcessCreationFormHasErrors {

		@Test
		void processCreationFormWithBlankName() {
			given().formParam("name", "\t \n")
				.formParam("birthDate", "2015-02-12")
				.when()
				.post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("is required"));
		}

		@Test
		void processCreationFormWithDuplicateName() {
			given().formParam("name", "Rosy")
				.formParam("type", "dog")
				.formParam("birthDate", "2015-02-12")
				.when()
				.post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("is already in use"));
		}

		@Test
		void processCreationFormWithMissingPetType() {
			given().formParam("name", "Fido")
				.formParam("birthDate", "2015-02-12")
				.when()
				.post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("is required"));
		}

		@Test
		void processCreationFormWithInvalidBirthDate() {
			String futureBirthDate = LocalDate.now().plusMonths(1).toString();
			given().formParam("name", "Fido")
				.formParam("type", "dog")
				.formParam("birthDate", futureBirthDate)
				.when()
				.post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("invalid date"));
		}

		/**
		 * Was {@code processCreationFormWithDataIntegrityViolation}, which mocked
		 * {@code saveAndFlush} to throw. Panache static methods cannot be mocked with
		 * {@code @InjectMock}; the real database path is covered by
		 * {@code PetClinicConcurrencyTests}, and the exception classification is tested
		 * here.
		 */
		@Test
		void duplicatePetNameViolationIsDetected() {
			PersistenceException ex = new PersistenceException("could not execute statement",
					new RuntimeException("Unique index or primary key violation: \"PUBLIC.UNIQUE_OWNER_PET_NAME\""));
			assertThat(PetController.isDuplicatePetNameViolation(ex)).isTrue();
			assertThat(PetController.isDuplicatePetNameViolation(new PersistenceException("other"))).isFalse();
		}

		@Test
		void initUpdateForm() {
			given().when()
				.get("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("value=\"Rosy\""))
				.body(containsString("Update Pet"));
		}

	}

	@Test
	void processUpdateFormSuccess() {
		given().redirects()
			.follow(false)
			.formParam("name", "Lucky2")
			.formParam("type", "hamster")
			.formParam("birthDate", "2015-02-12")
			.when()
			.post("/owners/{ownerId}/pets/{petId}/edit", UPDATABLE_OWNER_ID, UPDATABLE_PET_ID)
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + UPDATABLE_OWNER_ID));
	}

	@Test
	void processUpdateFormWithSameName() {
		given().redirects()
			.follow(false)
			.formParam("name", "George") // same name as existing pet
			.formParam("type", "snake")
			.formParam("birthDate", "2010-01-20")
			.when()
			.post("/owners/{ownerId}/pets/{petId}/edit", SAME_NAME_OWNER_ID, SAME_NAME_PET_ID)
			.then()
			.statusCode(303)
			.header("Location", endsWith("/owners/" + SAME_NAME_OWNER_ID));
	}

	@Nested
	class ProcessUpdateFormHasErrors {

		@Test
		void processUpdateFormWithDuplicateName() {
			// rename Jewel (id 4) to Rosy, the name of the owner's other pet
			given().formParam("name", "Rosy")
				.formParam("type", "dog")
				.formParam("birthDate", "2015-02-12")
				.when()
				.post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID + 1)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("is already in use"));
		}

		@Test
		void processUpdateFormWithInvalidBirthDate() {
			given().formParam("name", " ")
				.formParam("birthDate", "2015/02/12")
				.when()
				.post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("invalid date"));
		}

		@Test
		void processUpdateFormWithBlankName() {
			given().formParam("name", "  ")
				.formParam("birthDate", "2015-02-12")
				.when()
				.post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
				.then()
				.statusCode(200)
				.body(containsString(FORM))
				.body(containsString("is required"));
		}

	}

}
