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

/**
 * JUnit {@code @EnabledIf} condition replacing
 * {@code @Testcontainers(disabledWithoutDocker = true)}: database integration tests rely on
 * Quarkus Dev Services, which need a working container runtime.
 */
public final class DockerAvailable {

	private DockerAvailable() {
	}

	public static boolean isDockerAvailable() {
		try {
			Class<?> factory = Class.forName("org.testcontainers.DockerClientFactory");
			Object instance = factory.getMethod("instance").invoke(null);
			return (Boolean) factory.getMethod("isDockerAvailable").invoke(instance);
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
