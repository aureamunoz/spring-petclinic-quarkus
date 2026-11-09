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

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * Minimal replacement for Spring MVC flash attributes ({@code RedirectAttributes}): the
 * value is stored in a short-lived cookie on redirect and cleared when it is displayed.
 */
public final class Flash {

	public static final String MESSAGE = "flash-message";

	public static final String ERROR = "flash-error";

	private Flash() {
	}

	/**
	 * Redirect (303 See Other) to {@code location}, optionally carrying a flash attribute.
	 */
	public static Response redirect(String location, String flashName, String flashValue) {
		Response.ResponseBuilder builder = Response.seeOther(URI.create(location));
		if (flashName != null) {
			builder.cookie(new NewCookie.Builder(flashName)
				.value(URLEncoder.encode(flashValue, StandardCharsets.UTF_8))
				.path("/")
				.maxAge(60)
				.build());
		}
		return builder.build();
	}

	public static Response redirect(String location) {
		return redirect(location, null, null);
	}

	public static String decode(String cookieValue) {
		return cookieValue == null ? null : URLDecoder.decode(cookieValue, StandardCharsets.UTF_8);
	}

	/**
	 * Cookie that removes a consumed flash attribute.
	 */
	public static NewCookie clear(String flashName) {
		return new NewCookie.Builder(flashName).value("").path("/").maxAge(0).build();
	}

}
