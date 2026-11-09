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
package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.model.PageResult;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetService;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;

/**
 * Integration test of the persistence layer (was a {@code @DataJpaTest}, which rolls back
 * each test; {@code @TestTransaction} does the same in Quarkus).
 * <p>
 * Spring Data repository calls are replaced by Panache Active Record static methods;
 * {@code save(...)}/{@code saveAndFlush(...)} on managed entities become
 * {@code persist()}/{@code flush()}.
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
 */
@QuarkusTest
@TestTransaction
class ClinicServiceTests {

	@Inject
	VetService vets;

	// was Pageable.unpaged()
	private static PageResult<Owner> findByLastName(String lastName) {
		return Owner.findByLastNameStartingWith(lastName, 0, Integer.MAX_VALUE);
	}

	@Test
	void shouldFindOwnersByLastName() {
		PageResult<Owner> owners = findByLastName("Davis");
		assertThat(owners.content()).hasSize(2);

		owners = findByLastName("Daviss");
		assertThat(owners.content()).isEmpty();
	}

	@Test
	void shouldFindSingleOwnerWithPet() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(1);
		assertThat(optionalOwner).isPresent();
		Owner owner = optionalOwner.get();
		assertThat(owner.lastName).startsWith("Franklin");
		assertThat(owner.getPets()).hasSize(1);
		assertThat(owner.getPets().get(0).type).isNotNull();
		assertThat(owner.getPets().get(0).type.name).isEqualTo("cat");
	}

	@Test
	void shouldInsertOwner() {
		PageResult<Owner> owners = findByLastName("Schultz");
		int found = (int) owners.totalElements();

		Owner owner = new Owner();
		owner.firstName = "Sam";
		owner.lastName = "Schultz";
		owner.address = "4, Evans Street";
		owner.city = "Wollongong";
		owner.telephone = "4444444444";
		owner.persist();
		assertThat(owner.id).isNotZero();

		owners = findByLastName("Schultz");
		assertThat(owners.totalElements()).isEqualTo(found + 1);
	}

	@Test
	void shouldUpdateOwner() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(1);
		assertThat(optionalOwner).isPresent();
		Owner owner = optionalOwner.get();
		String oldLastName = owner.lastName;
		String newLastName = oldLastName + "X";

		owner.lastName = newLastName;
		Owner.flush();

		// retrieving new name from database
		optionalOwner = Owner.findOwnerById(1);
		assertThat(optionalOwner).isPresent();
		owner = optionalOwner.get();
		assertThat(owner.lastName).isEqualTo(newLastName);
	}

	@Test
	void shouldFindAllPetTypes() {
		Collection<PetType> petTypes = PetType.findPetTypes();

		PetType petType1 = EntityUtils.getById(petTypes, PetType.class, 1);
		assertThat(petType1.name).isEqualTo("cat");
		PetType petType4 = EntityUtils.getById(petTypes, PetType.class, 4);
		assertThat(petType4.name).isEqualTo("snake");
	}

	@Test
	void shouldInsertPetIntoDatabaseAndGenerateId() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		int found = owner6.getPets().size();

		Pet pet = new Pet();
		pet.name = "bowser";
		Collection<PetType> types = PetType.findPetTypes();
		pet.type = EntityUtils.getById(types, PetType.class, 2);
		pet.birthDate = LocalDate.now();
		owner6.addPet(pet);
		assertThat(owner6.getPets()).hasSize(found + 1);

		Owner.flush();

		optionalOwner = Owner.findOwnerById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();
		assertThat(owner6.getPets()).hasSize(found + 1);
		// checks that id has been generated
		pet = owner6.getPet("bowser");
		assertThat(pet.id).isNotNull();
	}

	@Test
	void shouldUpdatePetName() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		String oldName = pet7.name;

		String newName = oldName + "X";
		pet7.name = newName;
		Owner.flush();

		optionalOwner = Owner.findOwnerById(6);
		assertThat(optionalOwner).isPresent();
		owner6 = optionalOwner.get();
		pet7 = owner6.getPet(7);
		assertThat(pet7.name).isEqualTo(newName);
	}

	@Test
	void shouldFindVets() {
		Collection<Vet> vets = this.vets.findAll();

		Vet vet = EntityUtils.getById(vets, Vet.class, 3);
		assertThat(vet.lastName).isEqualTo("Douglas");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
		assertThat(vet.getSpecialties().get(0).name).isEqualTo("dentistry");
		assertThat(vet.getSpecialties().get(1).name).isEqualTo("surgery");
	}

	@Test
	void shouldAddNewVisitForPet() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		int found = pet7.getVisits().size();
		Visit visit = new Visit();
		visit.description = "test";

		owner6.addVisit(pet7.id, visit);
		Owner.flush();

		assertThat(pet7.getVisits()) //
			.hasSize(found + 1) //
			.allMatch(value -> value.id != null);
	}

	@Test
	void shouldFindVisitsByPetId() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(6);
		assertThat(optionalOwner).isPresent();
		Owner owner6 = optionalOwner.get();

		Pet pet7 = owner6.getPet(7);
		Collection<Visit> visits = pet7.getVisits();

		assertThat(visits) //
			.hasSize(2) //
			.element(0)
			.extracting(v -> v.date)
			.isNotNull();
	}

	@Test
	void shouldFailToInsertDuplicatePetNameForSameOwner() {
		Optional<Owner> optionalOwner = Owner.findOwnerById(1);
		assertThat(optionalOwner).isPresent();
		Owner owner = optionalOwner.get();

		Pet pet1 = new Pet();
		pet1.name = "DuplicatePetName";
		Collection<PetType> types = PetType.findPetTypes();
		pet1.type = EntityUtils.getById(types, PetType.class, 1);
		pet1.birthDate = LocalDate.now();
		owner.addPet(pet1);
		Owner.flush();

		Pet pet2 = new Pet();
		pet2.name = "duplicatepetname"; // Case-insensitive duplicate name
		pet2.type = EntityUtils.getById(types, PetType.class, 1);
		pet2.birthDate = LocalDate.now();
		owner.addPet(pet2);

		// was DataIntegrityViolationException
		// lambda, not Owner::flush: a method reference binds to PanacheEntityBase.flush and bypasses Panache enhancement
		assertThrows(PersistenceException.class, () -> Owner.flush());
	}

	@Test
	void shouldAllowSamePetNameForDifferentOwners() {
		Collection<PetType> types = PetType.findPetTypes();
		PetType catType = EntityUtils.getById(types, PetType.class, 1);

		Optional<Owner> owner1Opt = Owner.findOwnerById(1);
		assertThat(owner1Opt).isPresent();
		Owner owner1 = owner1Opt.get();
		Pet pet1 = new Pet();
		pet1.name = "SamePetName";
		pet1.type = catType;
		pet1.birthDate = LocalDate.now();
		owner1.addPet(pet1);
		Owner.flush();

		Optional<Owner> owner2Opt = Owner.findOwnerById(2);
		assertThat(owner2Opt).isPresent();
		Owner owner2 = owner2Opt.get();
		Pet pet2 = new Pet();
		pet2.name = "samepetname"; // Case-insensitive duplicate name, but for a
										// different owner
		pet2.type = catType;
		pet2.birthDate = LocalDate.now();
		owner2.addPet(pet2);

		// Saving for different owner should succeed
		Owner.flush();

		// Verify both exist
		assertThat(owner1.getPet("SamePetName")).isNotNull();
		assertThat(owner2.getPet("samepetname")).isNotNull();
	}

}
