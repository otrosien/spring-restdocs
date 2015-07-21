package org.springframework.restdocs.templates.mustache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.restdocs.mustache.Mustache;
import org.springframework.restdocs.mustache.Mustache.Compiler;
import org.springframework.restdocs.mustache.Template.Fragment;
import org.springframework.restdocs.templates.Template;
import org.springframework.restdocs.templates.TemplateEngine;
import org.springframework.restdocs.templates.TemplateResourceResolver;

public class MustacheTemplateEngine implements TemplateEngine {

	private final Compiler compiler = Mustache.compiler().escapeHTML(false);

	private final TemplateResourceResolver templateResourceResolver;

	private final Map<String, Object> defaultContext = new HashMap<>();

	public MustacheTemplateEngine(TemplateResourceResolver templateResourceResolver) {
		this.templateResourceResolver = templateResourceResolver;

		this.defaultContext.put("codeBlock", new Mustache.Lambda() {

			@Override
			public void execute(Fragment frag, Writer out) throws IOException {
				StringWriter fragmentWriter = new StringWriter();
				frag.execute(fragmentWriter);
				Template codeBlock = MustacheTemplateEngine.this
						.compileTemplate("code-block");
				@SuppressWarnings("unchecked")
				Map<String, Object> context = (Map<String, Object>) frag.context();
				Map<String, Object> nestedContext = new HashMap<String, Object>();
				nestedContext.putAll(context);
				nestedContext.put("code", fragmentWriter.toString());
				out.append(codeBlock.render(nestedContext));
			}
		});
	}

	@Override
	public Template compileTemplate(String name) throws IOException {
		Resource templateResource = this.templateResourceResolver
				.resolveTemplateResource(name);
		return new MustacheTemplate(this.compiler.compile(new InputStreamReader(
				templateResource.getInputStream())), this.defaultContext);
	}

}
