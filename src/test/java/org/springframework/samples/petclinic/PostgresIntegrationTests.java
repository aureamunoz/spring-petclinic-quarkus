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
 * Runs against a PostgreSQL database started by Quarkus Dev Services (replaces Spring
 * Boot's docker-compose support). Disabled when no container runtime is available.
 * <p>
 * REMOVED: the {@code main} method and the {@code PropertiesLogger} Spring
 * {@code ApplicationListener}. To run the application against PostgreSQL locally use
 * {@code ./mvnw quarkus:dev -Dquarkus.profile=postgres} (or Dev Services by overriding
 * {@code quarkus.datasource.db-kind=postgresql}).
 */
@QuarkusTest
@TestProfile(PostgresIntegrationTests.PostgresProfile.class)
@EnabledIf("org.springframework.samples.petclinic.DockerAvailable#isDockerAvailable")
public class PostgresIntegrationTests {

	public static class PostgresProfile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of("quarkus.datasource.db-kind", "postgresql", "quarkus.flyway.locations",
					"db/migration/postgres");
		}

	}

	@Inject
	VetService vets;

	@Test
	void findAll() throws Exception {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		given().when().get("/owners/1").then().statusCode(200);
	}

}
