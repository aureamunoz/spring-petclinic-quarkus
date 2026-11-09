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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.system.FormErrors;

/**
 * Test class for {@link PetValidator}
 *
 * @author Wick Dynex
 */
public class PetValidatorTests {

	private PetValidator petValidator;

	private Pet pet;

	private PetType petType;

	private FormErrors errors;

	private static final String petName = "Buddy";

	private static final String petTypeName = "Dog";

	private static final LocalDate petBirthDate = LocalDate.of(1990, 1, 1);

	@BeforeEach
	void setUp() {
		petValidator = new PetValidator();
		pet = new Pet();
		petType = new PetType();
		errors = new FormErrors(); // was MapBindingResult
	}

	@Test
	void supportsPetClass() {
		assertTrue(petValidator.supports(Pet.class));
	}

	@Test
	void doesNotSupportNonPetClass() {
		assertFalse(petValidator.supports(String.class));
	}

	@Test
	void validate() {
		petType.name = petTypeName;
		pet.name = petName;
		pet.type = petType;
		pet.birthDate = petBirthDate;

		petValidator.validate(pet, errors);

		assertFalse(errors.hasErrors());
	}

	@Nested
	class ValidateHasErrors {

		@Test
		void validateWithInvalidPetName() {
			petType.name = petTypeName;
			pet.name = "";
			pet.type = petType;
			pet.birthDate = petBirthDate;

			petValidator.validate(pet, errors);

			assertTrue(errors.hasFieldErrors("name"));
		}

		@Test
		void validateWithInvalidPetType() {
			pet.name = petName;
			pet.type = null;
			pet.birthDate = petBirthDate;

			petValidator.validate(pet, errors);

			assertTrue(errors.hasFieldErrors("type"));
		}

		@Test
		void validateWithInvalidBirthDate() {
			petType.name = petTypeName;
			pet.name = petName;
			pet.type = petType;
			pet.birthDate = null;

			petValidator.validate(pet, errors);

			assertTrue(errors.hasFieldErrors("birthDate"));
		}

		@Test
		void validateWithLongPetName() {
			petType.name = petTypeName;
			pet.name = "A".repeat(31);
			pet.type = petType;
			pet.birthDate = petBirthDate;

			petValidator.validate(pet, errors);

			assertTrue(errors.hasFieldErrors("name"));
		}

	}

}
