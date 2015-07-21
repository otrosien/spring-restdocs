package org.springframework.restdocs.templates;

import java.io.IOException;

public interface TemplateEngine {

	Template compileTemplate(String path) throws IOException;

}
