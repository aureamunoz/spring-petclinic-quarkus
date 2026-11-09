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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test class for the {@link VetController}
 * <p>
 * Migrated from a {@code @WebMvcTest} with a mocked {@code VetRepository} to a
 * {@code @QuarkusTest} against the seeded data (6 vets, 5 per page = 2 pages).
 */
@QuarkusTest
class VetControllerTests {

	@Test
	void showVetListHtml() {
		given().when()
			.get("/vets.html?page=1")
			.then()
			.statusCode(200)
			.body(containsString("id=\"vets\""))
			.body(containsString("James Carter"))
			.body(containsString("radiology"));
	}

	@Test
	void showVetListRedirectsOutOfBoundsPageToFirstPage() {
		for (int page : List.of(0, 3)) {
			given().redirects()
				.follow(false)
				.queryParam("page", page)
				.when()
				.get("/vets.html")
				.then()
				.statusCode(303)
				.header("Location", endsWith("/vets.html?page=1"));
		}
	}

	@Test
	void showResourcesVetList() {
		given().accept("application/json")
			.when()
			.get("/vets")
			.then()
			.statusCode(200)
			.contentType("application/json")
			.body("vetList.id", hasItem(1));
	}

}
