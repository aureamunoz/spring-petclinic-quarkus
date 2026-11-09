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
package org.springframework.samples.petclinic;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.samples.petclinic.vet.VetService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

/**
 * Runs against a MySQL database started by Quarkus Dev Services (replaces the Spring
 * Testcontainers {@code @ServiceConnection MySQLContainer}). Disabled when no container
 * runtime is available, like the Spring version.
 */
@QuarkusTest
@TestProfile(MySqlIntegrationTests.MySqlProfile.class)
@EnabledIf("org.springframework.samples.petclinic.DockerAvailable#isDockerAvailable")
class MySqlIntegrationTests {

	public static class MySqlProfile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			// db-kind is a build-time property: test profiles re-augment the application
			return Map.of("quarkus.datasource.db-kind", "mysql", "quarkus.datasource.devservices.image-name",
					"mysql:9.7", "quarkus.flyway.locations", "db/migration/mysql");
		}

	}

	@Inject
	VetService vets;

	@Test
	void findAll() {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		given().when().get("/owners/1").then().statusCode(200);
	}

}
