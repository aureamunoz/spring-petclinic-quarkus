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

import java.util.Locale;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the locale of the current HTTP request. Populated by {@link LocaleFilters}.
 */
@RequestScoped
public class CurrentLocale {

	private Locale locale = Messages.DEFAULT_LOCALE;

	private boolean changed;

	public Locale getLocale() {
		return locale;
	}

	void setLocale(Locale locale, boolean changed) {
		this.locale = locale;
		this.changed = changed;
	}

	boolean isChanged() {
		return changed;
	}

}
