package org.springframework.restdocs.templates.mustache;

import java.util.HashMap;
import java.util.Map;

import org.springframework.restdocs.templates.Template;

public class MustacheTemplate implements Template {

	private final org.springframework.restdocs.mustache.Template delegate;

	private final Map<String, Object> defaultContext;

	public MustacheTemplate(org.springframework.restdocs.mustache.Template delegate,
			Map<String, Object> defaultContext) {
		this.delegate = delegate;
		this.defaultContext = defaultContext;
	}

	@Override
	public String render(Map<String, Object> context) {
		Map<String, Object> executionContext = new HashMap<>();
		executionContext.putAll(this.defaultContext);
		executionContext.putAll(context);
		return this.delegate.execute(executionContext);
	}

}
