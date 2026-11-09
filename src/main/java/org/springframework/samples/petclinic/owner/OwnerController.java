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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.jboss.resteasy.reactive.RestCookie;
import org.springframework.samples.petclinic.model.PageResult;
import org.springframework.samples.petclinic.system.Flash;
import org.springframework.samples.petclinic.system.FormErrors;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

/**
 * Migrated from a Spring MVC {@code @Controller} to a JAX-RS resource rendering Qute
 * templates.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Wick Dynex
 */
@Path("/owners")
@Produces(MediaType.TEXT_HTML)
public class OwnerController {

	private static final int PAGE_SIZE = 5;

	@CheckedTemplate(basePath = "owners")
	public static class Templates {

		public static native TemplateInstance createOrUpdateOwnerForm(Owner owner, FormErrors errors);

		public static native TemplateInstance findOwners(Owner owner, FormErrors errors);

		public static native TemplateInstance ownersList(List<Owner> listOwners, int currentPage, int totalPages,
				long totalItems);

		public static native TemplateInstance ownerDetails(Owner owner, String message, String error);

	}

	@Inject
	Validator validator;

	/**
	 * Replaces the Spring {@code @ModelAttribute("owner")} method.
	 */
	static Owner findOwner(int ownerId) {
		return Owner.findOwnerById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId
					+ ". Please ensure the ID is correct " + "and the owner exists in the database."));
	}

	/**
	 * Binds the editable owner fields. Replaces Spring data binding with
	 * {@code @InitBinder setDisallowedFields("id", "*.id")}: the id is never bound.
	 */
	static void bind(Owner owner, MultivaluedMap<String, String> form) {
		if (form.containsKey("firstName")) {
			owner.firstName = form.getFirst("firstName");
		}
		if (form.containsKey("lastName")) {
			owner.lastName = form.getFirst("lastName");
		}
		if (form.containsKey("address")) {
			owner.address = form.getFirst("address");
		}
		if (form.containsKey("city")) {
			owner.city = form.getFirst("city");
		}
		if (form.containsKey("telephone")) {
			owner.telephone = form.getFirst("telephone");
		}
	}

	@GET
	@Path("/new")
	public TemplateInstance initCreationForm() {
		return Templates.createOrUpdateOwnerForm(new Owner(), new FormErrors());
	}

	@POST
	@Path("/new")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public Response processCreationForm(MultivaluedMap<String, String> form) {
		Owner owner = new Owner();
		bind(owner, form);
		FormErrors errors = new FormErrors().addViolations(validator.validate(owner));
		if (errors.hasErrors()) {
			return Response.ok(Templates.createOrUpdateOwnerForm(owner, errors)).build();
		}
		owner.persist();
		return Flash.redirect("/owners/" + owner.id, Flash.MESSAGE, "New Owner Created");
	}

	@GET
	@Path("/find")
	public TemplateInstance initFindForm() {
		return Templates.findOwners(new Owner(), new FormErrors());
	}

	@GET
	public Response processFindForm(@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("lastName") String lastName) {
		// allow parameterless GET request for /owners to return all records
		if (lastName == null) {
			lastName = ""; // empty string signifies broadest possible search
		}
		else {
			lastName = lastName.strip();
		}

		// find owners by last name
		PageResult<Owner> ownersResults = Owner.findByLastNameStartingWith(lastName, Math.max(page, 1) - 1,
				PAGE_SIZE);
		if (page < 1 || page > Math.max(ownersResults.totalPages(), 1)) {
			String location = "/owners?page=1";
			if (!lastName.isEmpty()) {
				location += "&lastName=" + URLEncoder.encode(lastName, StandardCharsets.UTF_8);
			}
			return Flash.redirect(location);
		}

		if (ownersResults.isEmpty()) {
			// no owners found
			Owner owner = new Owner();
			owner.lastName = lastName;
			FormErrors errors = new FormErrors();
			errors.rejectValue("lastName", "notFound");
			return Response.ok(Templates.findOwners(owner, errors)).build();
		}

		if (ownersResults.totalElements() == 1) {
			// 1 owner found
			return Flash.redirect("/owners/" + ownersResults.content().get(0).id);
		}

		// multiple owners found
		return Response
			.ok(Templates.ownersList(ownersResults.content(), page, ownersResults.totalPages(),
					ownersResults.totalElements()))
			.build();
	}

	@GET
	@Path("/{ownerId}/edit")
	public TemplateInstance initUpdateOwnerForm(@PathParam("ownerId") int ownerId) {
		return Templates.createOrUpdateOwnerForm(findOwner(ownerId), new FormErrors());
	}

	@POST
	@Path("/{ownerId}/edit")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public Response processUpdateOwnerForm(@PathParam("ownerId") int ownerId, MultivaluedMap<String, String> form) {
		Owner owner = findOwner(ownerId);
		bind(owner, form);
		FormErrors errors = new FormErrors().addViolations(validator.validate(owner));
		if (errors.hasErrors()) {
			// the owner is a managed entity: make sure the invalid values are not flushed
			QuarkusTransaction.setRollbackOnly();
			return Response.ok(Templates.createOrUpdateOwnerForm(owner, errors)).build();
		}

		if (!Objects.equals(owner.id, ownerId)) {
			QuarkusTransaction.setRollbackOnly();
			return Flash.redirect("/owners/" + ownerId + "/edit", Flash.ERROR, "Owner ID mismatch. Please try again.");
		}

		// owner is managed: changes are flushed on commit (was owners.save(owner))
		return Flash.redirect("/owners/" + ownerId, Flash.MESSAGE, "Owner Values Updated");
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 */
	@GET
	@Path("/{ownerId}")
	public Response showOwner(@PathParam("ownerId") int ownerId, @RestCookie(Flash.MESSAGE) String message,
			@RestCookie(Flash.ERROR) String error) {
		Owner owner = Owner.findOwnerById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException(
					"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));
		Response.ResponseBuilder response = Response
			.ok(Templates.ownerDetails(owner, Flash.decode(message), Flash.decode(error)));
		if (message != null) {
			response.cookie(Flash.clear(Flash.MESSAGE));
		}
		if (error != null) {
			response.cookie(Flash.clear(Flash.ERROR));
		}
		return response.build();
	}

}
