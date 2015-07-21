package org.springframework.restdocs.templates;

import java.util.Map;

public interface Template {

	String render(Map<String, Object> context);

}
