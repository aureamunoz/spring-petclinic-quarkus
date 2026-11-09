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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.owner.Owner;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class PetClinicConcurrencyTests {

	// Was owner 1 in the Spring test. Quarkus tests share a single database instance, and
	// ClinicServiceTests asserts that owner 1 has exactly one pet, so use an owner no
	// other test touches.
	private static final int OWNER_ID = 8;

	private static Owner loadOwner() {
		// new transaction = fresh persistence context (no stale first-level cache)
		return QuarkusTransaction.requiringNew().call(() -> Owner.findOwnerById(OWNER_ID).orElseThrow());
	}

	@Test
	public void testDuplicatePetNameRaceConditionIsBlocked() throws Exception {
		Owner owner = loadOwner();
		int initialPetCount = owner.getPets().size();
		String duplicatePetName = "ConcurrencyTestPet";

		// Ensure duplicate pet name does not exist yet
		assertThat(owner.getPet(duplicatePetName)).isNull();

		int threadCount = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch readyLatch = new CountDownLatch(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				readyLatch.countDown();
				try {
					startLatch.await(); // Wait to start simultaneously
					Response response = given().redirects()
						.follow(false)
						.formParam("name", duplicatePetName)
						.formParam("birthDate", "2020-01-01")
						.formParam("type", "cat")
						.post("/owners/" + OWNER_ID + "/pets/new");
					String body = response.getBody().asString();
					// A redirect means the pet was added; a re-rendered form containing
					// the duplicate validation error means it was blocked
					if (response.getStatusCode() / 100 == 3 && !body.contains("is already in use")) {
						successCount.incrementAndGet();
					}
					else {
						failureCount.incrementAndGet();
					}
				}
				catch (Exception e) {
					failureCount.incrementAndGet();
				}
				finally {
					doneLatch.countDown();
				}
			});
		}

		try {
			boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
			assertThat(ready).isTrue();
			startLatch.countDown();
			boolean completed = doneLatch.await(20, TimeUnit.SECONDS);
			assertThat(completed).isTrue();
		}
		finally {
			executorService.shutdown();
		}

		Owner updatedOwner = loadOwner();
		int newPetCount = updatedOwner.getPets().size();

		System.out.println("--- Concurrency Test Assertions ---");
		System.out.println("Successful additions: " + successCount.get());
		System.out.println("Failed additions: " + failureCount.get());
		System.out.println("Original Pet Count: " + initialPetCount);
		System.out.println("Final Pet Count: " + newPetCount);

		// Exactly ONE concurrent request must succeed
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(newPetCount).isEqualTo(initialPetCount + 1);

		long countWithDuplicateName = updatedOwner.getPets()
			.stream()
			.filter(p -> duplicatePetName.equalsIgnoreCase(p.name))
			.count();
		assertThat(countWithDuplicateName).isEqualTo(1);
	}

}
