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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

import org.springframework.samples.petclinic.system.Flash;
import org.springframework.samples.petclinic.system.FormErrors;
import org.springframework.samples.petclinic.system.Messages;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
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
 * Migrated from a Spring MVC {@code @Controller @RequestMapping("/owners/{ownerId}")} to a
 * JAX-RS resource rendering Qute templates.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@Path("/owners")
@Produces(MediaType.TEXT_HTML)
public class PetController {

	@CheckedTemplate(basePath = "pets")
	public static class Templates {

		public static native TemplateInstance createOrUpdatePetForm(Owner owner, Pet pet, List<PetType> types,
				FormErrors errors);

	}

	private final PetTypeFormatter petTypeFormatter = new PetTypeFormatter();

	private final PetValidator petValidator = new PetValidator();

	/**
	 * Binds the editable pet fields (replaces Spring data binding,
	 * {@code @InitBinder("pet")} with {@code setDisallowedFields("id", "*.id")} and the
	 * {@link PetTypeFormatter} conversion). Conversion failures are reported as
	 * {@code typeMismatch} errors, like Spring does.
	 */
	void bind(Pet pet, MultivaluedMap<String, String> form, FormErrors errors) {
		if (form.containsKey("name")) {
			pet.name = form.getFirst("name");
		}
		if (form.containsKey("birthDate")) {
			String value = form.getFirst("birthDate");
			if (value == null || value.isBlank()) {
				pet.birthDate = null;
			}
			else {
				try {
					pet.birthDate = LocalDate.parse(value.strip());
				}
				catch (DateTimeParseException ex) {
					pet.birthDate = null;
					errors.rejectValue("birthDate", "typeMismatch", Messages.get("typeMismatch.birthDate"));
				}
			}
		}
		if (form.containsKey("type")) {
			String value = form.getFirst("type");
			if (value == null || value.isBlank()) {
				pet.type = null;
			}
			else {
				try {
					pet.type = petTypeFormatter.parse(value, Messages.currentLocale());
				}
				catch (ParseException ex) {
					errors.rejectValue("type", "typeMismatch", Messages.get("notFound"));
				}
			}
		}
	}

	private Response form(Owner owner, Pet pet, FormErrors errors) {
		return Response.ok(Templates.createOrUpdatePetForm(owner, pet, PetType.findPetTypes(), errors)).build();
	}

	@GET
	@Path("/{ownerId}/pets/new")
	public TemplateInstance initCreationForm(@PathParam("ownerId") int ownerId) {
		Owner owner = OwnerController.findOwner(ownerId);
		Pet pet = new Pet();
		// REMOVED: owner.addPet(pet) from the Spring version; it only mutated the
		// request-scoped model and has no visible effect on the rendered form.
		return Templates.createOrUpdatePetForm(owner, pet, PetType.findPetTypes(), new FormErrors());
	}

	@POST
	@Path("/{ownerId}/pets/new")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public Response processCreationForm(@PathParam("ownerId") int ownerId, MultivaluedMap<String, String> form) {
		Owner owner = OwnerController.findOwner(ownerId);
		Pet pet = new Pet();
		FormErrors errors = new FormErrors();
		bind(pet, form, errors);
		petValidator.validate(pet, errors);

		if (PetValidator.hasText(pet.name) && pet.isNew() && owner.getPet(pet.name, true) != null) {
			errors.rejectValue("name", "duplicate");
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.birthDate != null && pet.birthDate.isAfter(currentDate)) {
			errors.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (errors.hasErrors()) {
			return form(owner, pet, errors);
		}

		try {
			owner.addPet(pet);
			Owner.flush(); // was owners.saveAndFlush(owner)
		}
		catch (PersistenceException ex) {
			if (!isDuplicatePetNameViolation(ex)) {
				throw ex;
			}
			QuarkusTransaction.setRollbackOnly();
			owner.getPets().remove(pet);
			errors.rejectValue("name", "duplicate");
			return form(owner, pet, errors);
		}
		return Flash.redirect("/owners/" + ownerId, Flash.MESSAGE, "New Pet has been Added");
	}

	@GET
	@Path("/{ownerId}/pets/{petId}/edit")
	public TemplateInstance initUpdateForm(@PathParam("ownerId") int ownerId, @PathParam("petId") int petId) {
		Owner owner = OwnerController.findOwner(ownerId);
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException("Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}
		return Templates.createOrUpdatePetForm(owner, pet, PetType.findPetTypes(), new FormErrors());
	}

	@POST
	@Path("/{ownerId}/pets/{petId}/edit")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public Response processUpdateForm(@PathParam("ownerId") int ownerId, @PathParam("petId") int petId,
			MultivaluedMap<String, String> form) {
		Owner owner = OwnerController.findOwner(ownerId);

		// Spring bound the request onto the pet loaded by @ModelAttribute("pet"); bind
		// onto a detached copy so that invalid input never reaches the managed entity.
		Pet pet = new Pet();
		pet.id = petId;
		Pet current = owner.getPet(petId);
		if (current != null) {
			pet.name = current.name;
			pet.birthDate = current.birthDate;
			pet.type = current.type;
		}
		FormErrors errors = new FormErrors();
		bind(pet, form, errors);
		petValidator.validate(pet, errors);

		String petName = pet.name;

		// checking if the pet name already exists for the owner
		if (PetValidator.hasText(petName)) {
			Pet existingPet = owner.getPet(petName, false);
			if (existingPet != null && !Objects.equals(existingPet.id, pet.id)) {
				errors.rejectValue("name", "duplicate");
			}
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.birthDate != null && pet.birthDate.isAfter(currentDate)) {
			errors.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (errors.hasErrors()) {
			return form(owner, pet, errors);
		}

		try {
			updatePetDetails(owner, pet);
		}
		catch (PersistenceException ex) {
			if (!isDuplicatePetNameViolation(ex)) {
				throw ex;
			}
			QuarkusTransaction.setRollbackOnly();
			errors.rejectValue("name", "duplicate");
			return form(owner, pet, errors);
		}
		return Flash.redirect("/owners/" + ownerId, Flash.MESSAGE, "Pet details has been edited");
	}

	/**
	 * Updates the pet details if it exists or adds a new pet to the owner.
	 * @param owner The owner of the pet
	 * @param pet The pet with updated details
	 */
	private void updatePetDetails(Owner owner, Pet pet) {
		Integer id = pet.id;
		if (id == null) {
			// was org.springframework.util.Assert.state
			throw new IllegalStateException("'pet.getId()' must not be null");
		}
		Pet existingPet = owner.getPet(id);
		if (existingPet != null) {
			// Update existing pet's properties
			existingPet.name = pet.name;
			existingPet.birthDate = pet.birthDate;
			existingPet.type = pet.type;
		}
		else {
			pet.id = null;
			owner.addPet(pet);
		}
		Owner.flush(); // was owners.saveAndFlush(owner)
	}

	/**
	 * Was {@code isDuplicatePetNameViolation(DataIntegrityViolationException)}. Hibernate
	 * reports the database constraint name somewhere in the cause chain.
	 */
	static boolean isDuplicatePetNameViolation(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			String message = t.getMessage();
			if (message != null && message.toLowerCase().contains("unique_owner_pet_name")) {
				return true;
			}
			if (t.getCause() == t) {
				break;
			}
		}
		return false;
	}

}
