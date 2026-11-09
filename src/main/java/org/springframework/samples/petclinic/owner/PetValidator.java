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

import org.springframework.samples.petclinic.system.FormErrors;

/**
 * <code>Validator</code> for <code>Pet</code> forms.
 * <p>
 * We're not using Bean Validation annotations here because it is easier to define such
 * validation rule in Java.
 * </p>
 * Formerly implemented {@code org.springframework.validation.Validator}; now reports into
 * {@link FormErrors}.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class PetValidator {

	private static final String REQUIRED = "required";

	private static final int MAX_NAME_LENGTH = 30;

	public void validate(Object obj, FormErrors errors) {
		Pet pet = (Pet) obj;
		String name = pet.name;
		// name validation
		if (!hasText(name)) {
			errors.rejectValue("name", REQUIRED);
		}
		else if (name.length() > MAX_NAME_LENGTH) {
			errors.rejectValue("name", "size", "Name must be no more than 30 characters");
		}

		// type validation
		if (pet.isNew() && pet.type == null) {
			errors.rejectValue("type", REQUIRED);
		}

		// birth date validation
		if (pet.birthDate == null) {
			errors.rejectValue("birthDate", REQUIRED);
		}
	}

	/**
	 * This Validator validates *just* Pet instances
	 */
	public boolean supports(Class<?> clazz) {
		return Pet.class.isAssignableFrom(clazz);
	}

	/** Replacement for {@code org.springframework.util.StringUtils.hasText}. */
	static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

}
