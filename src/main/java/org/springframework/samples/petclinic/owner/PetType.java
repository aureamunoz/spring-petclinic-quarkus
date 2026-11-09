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

import java.util.List;

import org.springframework.samples.petclinic.model.NamedEntity;

import io.quarkus.panache.common.Sort;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Juergen Hoeller Can be Cat, Dog, Hamster...
 */
@Entity
@Table(name = "types")
public class PetType extends NamedEntity {

	/**
	 * Retrieve all {@link PetType}s from the data store, ordered by name (formerly
	 * {@code PetTypeRepository.findPetTypes()} with
	 * {@code @Query("SELECT ptype FROM PetType ptype ORDER BY ptype.name")}).
	 * @return a List of {@link PetType}s.
	 */
	public static List<PetType> findPetTypes() {
		return listAll(Sort.by("name"));
	}

}
