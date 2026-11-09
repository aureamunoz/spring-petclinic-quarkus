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

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Replaces Spring Boot's {@code BasicErrorController}: renders {@code error.html} for
 * browsers and a JSON error body ({@code timestamp, status, error, message, path}) for
 * other clients.
 */
public class ErrorMapper {

	private static final Logger LOG = Logger.getLogger(ErrorMapper.class);

	@ServerExceptionMapper
	public Response mapException(Exception exception, HttpHeaders headers, UriInfo uriInfo) {
		int status = 500;
		if (exception instanceof WebApplicationException wae) {
			Response original = wae.getResponse();
			if (original.getStatus() < 400) {
				return original;
			}
			status = original.getStatus();
		}
		else {
			LOG.error("Unhandled exception for " + uriInfo.getPath(), exception);
		}
		Response.Status statusInfo = Response.Status.fromStatusCode(status);
		String error = statusInfo != null ? statusInfo.getReasonPhrase() : "Error";
		String message = exception.getMessage();

		if (acceptsHtml(headers)) {
			String statusMessage = switch (status) {
				case 404 -> Messages.get("error.404");
				case 500 -> Messages.get("error.500");
				default -> Messages.get("error.general");
			};
			return Response.status(status)
				.type(MediaType.TEXT_HTML_TYPE)
				.entity(Templates.error(status, statusMessage, message == null ? "" : message))
				.build();
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", OffsetDateTime.now().toString());
		body.put("status", status);
		body.put("error", error);
		body.put("message", message);
		body.put("path", uriInfo.getPath());
		return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(body).build();
	}

	private static boolean acceptsHtml(HttpHeaders headers) {
		return headers.getAcceptableMediaTypes()
			.stream()
			.anyMatch(type -> !type.isWildcardType() && type.isCompatible(MediaType.TEXT_HTML_TYPE));
	}

}
