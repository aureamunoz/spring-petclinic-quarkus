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
package org.springframework.samples.petclinic.vet;

import java.util.List;

import org.springframework.samples.petclinic.model.PageResult;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Cached read access to {@link Vet}s. Replaces the Spring Data
 * {@code VetRepository extends Repository<Vet, Integer>}: the queries are Panache Active
 * Record calls on {@link Vet}, and this CDI bean only exists because caching interceptors
 * ({@code @CacheResult}) cannot apply to static entity methods.
 * <p>
 * REMOVED: {@code @Transactional(readOnly = true)}; Quarkus does not need a transaction
 * for read-only queries and JTA has no read-only flag.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
@ApplicationScoped
public class VetService {

	/**
	 * Retrieve all <code>Vet</code>s from the data store.
	 * @return a <code>List</code> of <code>Vet</code>s
	 */
	@CacheResult(cacheName = "vets")
	public List<Vet> findAll() {
		return Vet.listAll();
	}

	/**
	 * Retrieve all <code>Vet</code>s from data store in pages.
	 * @param pageIndex 0-based page index
	 * @param pageSize page size
	 */
	@CacheResult(cacheName = "vets")
	public PageResult<Vet> findAll(int pageIndex, int pageSize) {
		return PageResult.of(Vet.findAll(), pageIndex, pageSize);
	}

}
