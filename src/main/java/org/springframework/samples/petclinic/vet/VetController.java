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
import org.springframework.samples.petclinic.system.Flash;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Migrated from a Spring MVC {@code @Controller} to a JAX-RS resource.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Path("/")
public class VetController {

	private static final int PAGE_SIZE = 5;

	@CheckedTemplate(basePath = "vets")
	public static class Templates {

		public static native TemplateInstance vetList(List<Vet> listVets, int currentPage, int totalPages,
				long totalItems);

	}

	@Inject
	VetService vetService;

	@GET
	@Path("/vets.html")
	@Produces(MediaType.TEXT_HTML)
	public Response showVetList(@QueryParam("page") @DefaultValue("1") int page) {
		PageResult<Vet> paginated = vetService.findAll(Math.max(page, 1) - 1, PAGE_SIZE);
		if (page < 1 || page > Math.max(paginated.totalPages(), 1)) {
			return Flash.redirect("/vets.html?page=1");
		}
		return Response
			.ok(Templates.vetList(paginated.content(), page, paginated.totalPages(), paginated.totalElements()))
			.build();
	}

	@GET
	@Path("/vets")
	@Produces(MediaType.APPLICATION_JSON)
	public Vets showResourcesVetList() {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for JSon/Object mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(vetService.findAll());
		return vets;
	}

}
