package org.springframework.restdocs.templates;

import org.springframework.core.io.Resource;

public interface TemplateResourceResolver {

	public Resource resolveTemplateResource(String path);

}
