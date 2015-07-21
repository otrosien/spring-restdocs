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

package org.springframework.restdocs.hypermedia;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.restdocs.snippet.SnippetGenerationException;
import org.springframework.restdocs.snippet.SnippetWritingResultHandler;
import org.springframework.restdocs.templates.TemplateEngine;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Assert;

/**
 * A {@link SnippetWritingResultHandler} that produces a snippet documenting a RESTful
 * resource's links.
 * 
 * @author Andy Wilkinson
 */
public class LinkSnippetResultHandler extends SnippetWritingResultHandler {

	private final Map<String, LinkDescriptor> descriptorsByRel = new LinkedHashMap<>();

	private final Set<String> requiredRels = new HashSet<String>();

	private final LinkExtractor extractor;

	LinkSnippetResultHandler(String outputDir, LinkExtractor linkExtractor,
			List<LinkDescriptor> descriptors) {
		super(outputDir, "links");
		this.extractor = linkExtractor;
		for (LinkDescriptor descriptor : descriptors) {
			Assert.hasText(descriptor.getRel());
			Assert.hasText(descriptor.getDescription());
			this.descriptorsByRel.put(descriptor.getRel(), descriptor);
			if (!descriptor.isOptional()) {
				this.requiredRels.add(descriptor.getRel());
			}
		}
	}

	@Override
	protected void handle(MvcResult result, PrintWriter writer) throws IOException {
		validate(extractLinks(result));
		writeDocumentationSnippet(result, writer);
	}

	private Map<String, List<Link>> extractLinks(MvcResult result) throws IOException {
		if (this.extractor != null) {
			return this.extractor.extractLinks(result.getResponse());
		}
		else {
			String contentType = result.getResponse().getContentType();
			LinkExtractor extractorForContentType = LinkExtractors
					.extractorForContentType(contentType);
			if (extractorForContentType != null) {
				return extractorForContentType.extractLinks(result.getResponse());
			}
			throw new IllegalStateException(
					"No LinkExtractor has been provided and one is not available for the content type "
							+ contentType);

		}
	}

	private void validate(Map<String, List<Link>> links) {
		Set<String> actualRels = links.keySet();

		Set<String> undocumentedRels = new HashSet<String>(actualRels);
		undocumentedRels.removeAll(this.descriptorsByRel.keySet());

		Set<String> missingRels = new HashSet<String>(this.requiredRels);
		missingRels.removeAll(actualRels);

		if (!undocumentedRels.isEmpty() || !missingRels.isEmpty()) {
			String message = "";
			if (!undocumentedRels.isEmpty()) {
				message += "Links with the following relations were not documented: "
						+ undocumentedRels;
			}
			if (!missingRels.isEmpty()) {
				if (message.length() > 0) {
					message += ". ";
				}
				message += "Links with the following relations were not found in the response: "
						+ missingRels;
			}
			throw new SnippetGenerationException(message);
		}
	}

	private void writeDocumentationSnippet(MvcResult result, PrintWriter writer)
			throws IOException {
		TemplateEngine templateEngine = (TemplateEngine) result.getRequest()
				.getAttribute(TemplateEngine.class.getName());
		Map<String, Object> context = new HashMap<>();
		context.put("links", this.descriptorsByRel.values());
		writer.println(templateEngine.compileTemplate("links").render(context));
	}

}