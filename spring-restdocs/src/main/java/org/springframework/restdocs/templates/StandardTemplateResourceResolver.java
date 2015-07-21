package org.springframework.restdocs.templates;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class StandardTemplateResourceResolver implements TemplateResourceResolver {

	@Override
	public Resource resolveTemplateResource(String name) {
		ClassPathResource classPathResource = new ClassPathResource(
				"org/springframework/restdocs/templates/" + name + ".snippet");
		if (!classPathResource.exists()) {
			classPathResource = new ClassPathResource(
					"org/springframework/restdocs/templates/default-" + name + ".snippet");
			if (!classPathResource.exists()) {
				throw new IllegalStateException("Template named '" + name
						+ "' could not be resolved");
			}
		}
		return classPathResource;
	}

}
