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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

/**
 * @author Dave Syer
 */
class VetTests {

	@Test
	void serialization() throws Exception {
		Vet vet = new Vet();
		vet.firstName = "Zaphod";
		vet.lastName = "Beeblebrox";
		vet.id = 123;
		// was org.springframework.util.SerializationUtils
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(vet);
		}
		Vet other;
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			other = (Vet) in.readObject();
		}
		assertThat(other.firstName).isEqualTo(vet.firstName);
		assertThat(other.lastName).isEqualTo(vet.lastName);
		assertThat(other.id).isEqualTo(vet.id);
	}

}
