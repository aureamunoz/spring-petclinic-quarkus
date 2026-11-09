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
package org.springframework.samples.petclinic.model;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

/**
 * Immutable page of results. Replaces Spring Data's {@code Page<T>} (Panache returns a
 * {@link PanacheQuery} instead, which is not suitable for caching or passing to views).
 *
 * @param content the entities of the requested page
 * @param totalElements total number of matching entities
 * @param totalPages total number of pages
 */
public record PageResult<T>(List<T> content, long totalElements, int totalPages) {

	/**
	 * Executes the given query for the 0-based {@code pageIndex}.
	 */
	public static <T> PageResult<T> of(PanacheQuery<T> query, int pageIndex, int pageSize) {
		query.page(Page.of(pageIndex, pageSize));
		return new PageResult<>(query.list(), query.count(), query.pageCount());
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

}
