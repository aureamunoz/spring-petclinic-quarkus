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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.samples.petclinic.system.Flash;
import org.springframework.samples.petclinic.system.FormErrors;
import org.springframework.samples.petclinic.system.Messages;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
 * @author Dave Syer
 * @author Wick Dynex
 */
@Path("/owners")
@Produces(MediaType.TEXT_HTML)
public class VisitController {

	@CheckedTemplate(basePath = "pets")
	public static class Templates {

		public static native TemplateInstance createOrUpdateVisitForm(Owner owner, Pet pet, Visit visit,
				LocalDate minVisitDate, FormErrors errors);

	}

	@Inject
	Validator validator;

	/**
	 * Replaces the Spring {@code @ModelAttribute("visit") loadPetWithVisit(...)} method:
	 * make sure we always have fresh data.
	 */
	private static Pet loadPet(Owner owner, int ownerId, int petId) {
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException(
					"Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}
		return pet;
	}

	/** Replaces the Spring {@code @ModelAttribute("minVisitDate")} method. */
	private static LocalDate minVisitDate() {
		return LocalDate.now().plusDays(1);
	}

	@GET
	@Path("/{ownerId}/pets/{petId}/visits/new")
	public TemplateInstance initNewVisitForm(@PathParam("ownerId") int ownerId, @PathParam("petId") int petId) {
		Owner owner = OwnerController.findOwner(ownerId);
		Pet pet = loadPet(owner, ownerId, petId);
		return Templates.createOrUpdateVisitForm(owner, pet, new Visit(), minVisitDate(), new FormErrors());
	}

	@POST
	@Path("/{ownerId}/pets/{petId}/visits/new")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public Response processNewVisitForm(@PathParam("ownerId") int ownerId, @PathParam("petId") int petId,
			MultivaluedMap<String, String> form) {
		Owner owner = OwnerController.findOwner(ownerId);
		Pet pet = loadPet(owner, ownerId, petId);

		// Data binding (id is never bound, was @InitBinder setDisallowedFields("id", "*.id"))
		Visit visit = new Visit();
		FormErrors errors = new FormErrors();
		if (form.containsKey("date")) {
			String value = form.getFirst("date");
			try {
				visit.date = (value == null || value.isBlank()) ? null : LocalDate.parse(value.strip());
			}
			catch (DateTimeParseException ex) {
				visit.date = null;
				errors.rejectValue("date", "typeMismatch", Messages.get("typeMismatch.date"));
			}
		}
		if (form.containsKey("description")) {
			visit.description = form.getFirst("description");
		}
		errors.addViolations(validator.validate(visit));

		if (visit.date != null && !visit.date.isAfter(LocalDate.now())) {
			errors.rejectValue("date", "typeMismatch.visitDate");
		}

		if (errors.hasErrors()) {
			return Response.ok(Templates.createOrUpdateVisitForm(owner, pet, visit, minVisitDate(), errors)).build();
		}

		owner.addVisit(petId, visit);
		// owner is managed: the new visit is persisted by cascade on commit (was
		// owners.save(owner))
		return Flash.redirect("/owners/" + ownerId, Flash.MESSAGE, "Your visit has been booked");
	}

}
