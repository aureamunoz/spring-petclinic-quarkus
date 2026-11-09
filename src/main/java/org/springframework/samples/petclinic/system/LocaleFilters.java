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

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;

/**
 * Configures internationalization (i18n) support for the application.
 * <p>
 * Replaces the Spring MVC {@code WebConfiguration} ({@code SessionLocaleResolver} with an
 * English default + {@code LocaleChangeInterceptor} on the {@code lang} parameter). The
 * language can be switched with a URL parameter (e.g. {@code ?lang=de}) and is remembered
 * across requests in a cookie (Quarkus has no HTTP session by default).
 *
 * @author Anuj Ashok Potdar
 */
public class LocaleFilters {

	static final String PARAM_NAME = "lang";

	static final String COOKIE_NAME = "petclinic-lang";

	@Inject
	CurrentLocale currentLocale;

	@ServerRequestFilter
	public void resolveLocale(ContainerRequestContext request) {
		String lang = request.getUriInfo().getQueryParameters().getFirst(PARAM_NAME);
		if (lang != null && !lang.isBlank()) {
			currentLocale.setLocale(Locale.forLanguageTag(lang.replace('_', '-')), true);
			return;
		}
		Cookie cookie = request.getCookies().get(COOKIE_NAME);
		if (cookie != null && !cookie.getValue().isBlank()) {
			currentLocale.setLocale(Locale.forLanguageTag(cookie.getValue()), false);
		}
	}

	@ServerResponseFilter
	public void rememberLocale(ContainerResponseContext response) {
		if (currentLocale.isChanged()) {
			NewCookie cookie = new NewCookie.Builder(COOKIE_NAME).value(currentLocale.getLocale().toLanguageTag())
				.path("/")
				.build();
			response.getHeaders().add(HttpHeaders.SET_COOKIE, cookie);
		}
	}

}
