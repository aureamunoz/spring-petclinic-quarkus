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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.qute.TemplateExtension;

/**
 * Resolves localized messages from {@code messages/messages*.properties} (formerly the
 * Spring {@code MessageSource} configured with
 * {@code spring.messages.basename=messages/messages}).
 * <p>
 * In Qute templates use the {@code msg} namespace, e.g. {@code {msg:welcome}} (was
 * {@code th:text="#{welcome}"} in Thymeleaf).
 */
public final class Messages {

	static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

	private static final String BASENAME = "messages/messages";

	/**
	 * Fall back to the base bundle ({@code messages.properties}), never to the JVM default
	 * locale (same behavior as Spring's {@code fallbackToSystemLocale=false}).
	 */
	private static final ResourceBundle.Control CONTROL = ResourceBundle.Control
		.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

	private Messages() {
	}

	/**
	 * Returns the message for the given key in the locale of the current request, or the
	 * key itself if no such message exists.
	 */
	public static String get(String key) {
		return get(key, currentLocale());
	}

	public static String get(String key, Locale locale) {
		try {
			return ResourceBundle.getBundle(BASENAME, locale, CONTROL).getString(key);
		}
		catch (MissingResourceException ex) {
			return key;
		}
	}

	public static Locale currentLocale() {
		ArcContainer container = Arc.container();
		if (container != null && container.requestContext().isActive()) {
			return container.instance(CurrentLocale.class).get().getLocale();
		}
		return DEFAULT_LOCALE;
	}

	/**
	 * Qute template extension: {@code {msg:someKey}}.
	 */
	@TemplateExtension(namespace = "msg", matchName = TemplateExtension.ANY)
	static String message(String key) {
		return get(key);
	}

}
