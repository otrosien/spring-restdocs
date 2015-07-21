/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.restdocs.payload;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.restdocs.snippet.SnippetWritingResultHandler;
import org.springframework.restdocs.templates.TemplateEngine;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link SnippetWritingResultHandler} that produces a snippet documenting a RESTful
 * resource's request or response fields.
 * 
 * @author Andreas Evers
 * @author Andy Wilkinson
 */
public abstract class FieldSnippetResultHandler extends SnippetWritingResultHandler {

	private final Map<String, FieldDescriptor> descriptorsByPath = new LinkedHashMap<String, FieldDescriptor>();

	private final FieldTypeResolver fieldTypeResolver = new FieldTypeResolver();

	private final FieldValidator fieldValidator = new FieldValidator();

	private final ObjectMapper objectMapper = new ObjectMapper();

	private List<FieldDescriptor> fieldDescriptors;

	FieldSnippetResultHandler(String outputDir, String filename,
			List<FieldDescriptor> descriptors) {
		super(outputDir, filename + "-fields");
		for (FieldDescriptor descriptor : descriptors) {
			Assert.notNull(descriptor.getPath());
			Assert.hasText(descriptor.getDescription());
			this.descriptorsByPath.put(descriptor.getPath(), descriptor);
		}
		this.fieldDescriptors = descriptors;
	}

	@Override
	protected void handle(MvcResult result, PrintWriter writer) throws IOException {

		this.fieldValidator.validate(getPayloadReader(result), this.fieldDescriptors);

		final Object payload = extractPayload(result);

		TemplateEngine templateEngine = (TemplateEngine) result.getRequest()
				.getAttribute(TemplateEngine.class.getName());
		Map<String, Object> context = new HashMap<>();
		List<Map<String, String>> fields = new ArrayList<>();
		context.put("fields", fields);
		for (Entry<String, FieldDescriptor> entry : this.descriptorsByPath.entrySet()) {
			FieldDescriptor descriptor = entry.getValue();
			FieldType type = getFieldType(descriptor, payload);
			Map<String, String> fieldModel = new HashMap<>();
			fieldModel.put("path", entry.getKey());
			fieldModel.put("type", type.toString());
			fieldModel.put("description", descriptor.getDescription());
			fields.add(fieldModel);
		}
		writer.println(templateEngine.compileTemplate("fields").render(context));
	}

	private FieldType getFieldType(FieldDescriptor descriptor, Object payload) {
		if (descriptor.getType() != null) {
			return descriptor.getType();
		}
		else {
			try {
				return FieldSnippetResultHandler.this.fieldTypeResolver.resolveFieldType(
						descriptor.getPath(), payload);
			}
			catch (FieldDoesNotExistException ex) {
				String message = "Cannot determine the type of the field '"
						+ descriptor.getPath() + "' as it is not present in the"
						+ " payload. Please provide a type using"
						+ " FieldDescriptor.type(FieldType).";
				throw new FieldTypeRequiredException(message);
			}
		}
	}

	private Object extractPayload(MvcResult result) throws IOException {
		return this.objectMapper.readValue(getPayloadReader(result), Object.class);
	}

	protected abstract Reader getPayloadReader(MvcResult result) throws IOException;

}