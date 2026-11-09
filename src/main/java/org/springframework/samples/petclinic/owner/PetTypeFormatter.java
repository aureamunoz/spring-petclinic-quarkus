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

import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Parses and prints elements of type 'PetType'.
 * <p>
 * Formerly a Spring {@code Formatter<PetType>} registered as {@code @Component} and applied
 * automatically by Spring MVC data binding. Quarkus REST has no equivalent form-binding
 * conversion service, so {@link PetController} calls it explicitly when binding the
 * {@code type} form field.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Michael Isvy
 */
public class PetTypeFormatter {

	private final Supplier<? extends Collection<PetType>> types;

	public PetTypeFormatter() {
		this(PetType::findPetTypes);
	}

	PetTypeFormatter(Supplier<? extends Collection<PetType>> types) {
		this.types = types;
	}

	public String print(PetType petType, Locale locale) {
		String name = petType.name;
		return name != null ? name : "<null>";
	}

	public PetType parse(String text, Locale locale) throws ParseException {
		Collection<PetType> findPetTypes = this.types.get();
		for (PetType type : findPetTypes) {
			if (Objects.equals(type.name, text)) {
				return type;
			}
		}
		throw new ParseException("type not found: " + text, 0);
	}

}
