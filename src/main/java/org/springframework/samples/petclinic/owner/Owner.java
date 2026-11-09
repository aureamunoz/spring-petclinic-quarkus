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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.samples.petclinic.model.PageResult;
import org.springframework.samples.petclinic.model.Person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Simple JavaBean domain object representing an owner.
 * <p>
 * Panache Active Record: the queries of the former Spring Data {@code OwnerRepository}
 * are static methods of this entity.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Oliver Drotbohm
 * @author Wick Dynex
 */
@Entity
@Table(name = "owners")
public class Owner extends Person {

	@Column
	@NotBlank
	@Size(max = 255)
	public String address;

	@Column
	@NotBlank
	@Size(max = 80)
	public String city;

	@Column
	@NotBlank
	@Pattern(regexp = "\\d{10}", message = "{telephone.invalid}")
	public String telephone;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_id")
	@OrderBy("name")
	private final List<Pet> pets = new ArrayList<>();

	// ---- Queries (formerly OwnerRepository extends JpaRepository<Owner, Integer>) ----

	/**
	 * Retrieve {@link Owner}s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name (Spring Data
	 * {@code findByLastNameStartingWith(String, Pageable)}).
	 * @param lastName Value to search for
	 * @param pageIndex 0-based page index
	 * @param pageSize page size
	 * @return a page of matching {@link Owner}s (possibly empty)
	 */
	public static PageResult<Owner> findByLastNameStartingWith(String lastName, int pageIndex, int pageSize) {
		return PageResult.of(find("lastName like ?1 escape '\\'", escapeLike(lastName) + "%"), pageIndex, pageSize);
	}

	/**
	 * Retrieve an {@link Owner} from the data store by id (Spring Data
	 * {@code Optional<Owner> findById(Integer)}).
	 */
	public static Optional<Owner> findOwnerById(Integer id) {
		return findByIdOptional(id);
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	// ---- Domain logic ----

	public List<Pet> getPets() {
		return this.pets;
	}

	public void addPet(Pet pet) {
		if (pet == null) {
			return;
		}
		if (getPets().contains(pet)) {
			return;
		}
		for (Pet existingPet : getPets()) {
			if (!existingPet.isNew() && !pet.isNew() && Objects.equals(existingPet.id, pet.id)) {
				return;
			}
		}
		getPets().add(pet);
	}

	/**
	 * Return the Pet with the given name, or null if none found for this Owner.
	 * @param name to test
	 * @return the Pet with the given name, or null if no such Pet exists for this Owner
	 */
	public Pet getPet(String name) {
		return getPet(name, false);
	}

	/**
	 * Return the Pet with the given id, or null if none found for this Owner.
	 * @param id to test
	 * @return the Pet with the given id, or null if no such Pet exists for this Owner
	 */
	public Pet getPet(Integer id) {
		for (Pet pet : getPets()) {
			if (!pet.isNew()) {
				Integer compId = pet.id;
				if (Objects.equals(compId, id)) {
					return pet;
				}
			}
		}
		return null;
	}

	/**
	 * Return the Pet with the given name, or null if none found for this Owner.
	 * @param name to test
	 * @param ignoreNew whether to ignore new pets (pets that are not saved yet)
	 * @return the Pet with the given name, or null if no such Pet exists for this Owner
	 */
	public Pet getPet(String name, boolean ignoreNew) {
		for (Pet pet : getPets()) {
			String compName = pet.name;
			if (compName != null && compName.equalsIgnoreCase(name)) {
				if (!ignoreNew || !pet.isNew()) {
					return pet;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		// Was org.springframework.core.style.ToStringCreator
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [id = " + this.id + ", new = "
				+ this.isNew() + ", lastName = '" + this.lastName + "', firstName = '" + this.firstName
				+ "', address = '" + this.address + "', city = '" + this.city + "', telephone = '" + this.telephone
				+ "']";
	}

	/**
	 * Adds the given {@link Visit} to the {@link Pet} with the given identifier.
	 * @param petId the identifier of the {@link Pet}, must not be {@literal null}.
	 * @param visit the visit to add, must not be {@literal null}.
	 */
	public void addVisit(Integer petId, Visit visit) {
		// Was org.springframework.util.Assert.notNull (throws IllegalArgumentException)
		requireArgument(petId != null, "Pet identifier must not be null!");
		requireArgument(visit != null, "Visit must not be null!");
		Pet pet = getPet(petId);
		requireArgument(pet != null, "Invalid Pet identifier!");
		pet.addVisit(visit);
	}

	private static void requireArgument(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}

}
