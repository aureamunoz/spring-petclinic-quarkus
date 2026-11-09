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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.qute.TemplateData;
import jakarta.validation.ConstraintViolation;

/**
 * Collects field errors of a submitted form. Replaces Spring MVC's
 * {@code BindingResult}/{@code Errors}: Quarkus REST has no form-binding/validation
 * pipeline that renders back into a view, so resources bind and validate explicitly and
 * pass this object to the template.
 */
@TemplateData
public class FormErrors {

	private final Map<String, String> codes = new LinkedHashMap<>();

	private final Map<String, String> messages = new LinkedHashMap<>();

	/**
	 * Registers a field error. The message is resolved from the message bundle using the
	 * error code (like Spring's {@code rejectValue(field, code)}).
	 */
	public void rejectValue(String field, String code) {
		rejectValue(field, code, Messages.get(code));
	}

	public void rejectValue(String field, String code, String message) {
		codes.putIfAbsent(field, code);
		messages.putIfAbsent(field, message);
	}

	/**
	 * Adds Bean Validation constraint violations (the error code is the constraint
	 * annotation simple name, e.g. {@code NotBlank}, like in Spring).
	 */
	public <T> FormErrors addViolations(Set<ConstraintViolation<T>> violations) {
		for (ConstraintViolation<T> violation : violations) {
			rejectValue(violation.getPropertyPath().toString(),
					violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
					violation.getMessage());
		}
		return this;
	}

	public boolean hasErrors() {
		return !codes.isEmpty();
	}

	public boolean hasFieldErrors(String field) {
		return codes.containsKey(field);
	}

	public String getFieldError(String field) {
		return messages.get(field);
	}

	public String getFieldErrorCode(String field) {
		return codes.get(field);
	}

	public List<String> getAllErrors() {
		return new ArrayList<>(messages.values());
	}

}
