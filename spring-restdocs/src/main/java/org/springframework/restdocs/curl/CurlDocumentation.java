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

package org.springframework.restdocs.curl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.restdocs.snippet.SnippetWritingResultHandler;
import org.springframework.restdocs.templates.TemplateEngine;
import org.springframework.restdocs.util.DocumentableHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Static factory methods for documenting a RESTful API as if it were being driven using
 * the cURL command-line utility.
 *
 * @author Andy Wilkinson
 * @author Yann Le Guern
 * @author Dmitriy Mayboroda
 * @author Jonathan Pearlin
 */
public abstract class CurlDocumentation {

	private CurlDocumentation() {

	}

	/**
	 * Produces a documentation snippet containing the request formatted as a cURL command
	 *
	 * @param outputDir The directory to which snippet should be written
	 * @return the handler that will produce the snippet
	 */
	public static SnippetWritingResultHandler documentCurlRequest(String outputDir) {
		return new SnippetWritingResultHandler(outputDir, "curl-request") {

			@Override
			public void handle(MvcResult result, PrintWriter writer) throws IOException {
				String arguments = new CurlCommandArgumentsGenerator(result).generate();

				final Map<String, Object> context = new HashMap<String, Object>();
				context.put("arguments", arguments);
				context.put("language", "bash");

				TemplateEngine templateEngine = (TemplateEngine) result.getRequest()
						.getAttribute(TemplateEngine.class.getName());

				writer.println(templateEngine.compileTemplate("curl-request").render(
						context));
			}
		};
	}

	private static final class CurlCommandArgumentsGenerator {

		private static final String SCHEME_HTTP = "http";

		private static final String SCHEME_HTTPS = "https";

		private static final int STANDARD_PORT_HTTP = 80;

		private static final int STANDARD_PORT_HTTPS = 443;

		private final MvcResult result;

		CurlCommandArgumentsGenerator(MvcResult result) {
			this.result = result;
		}

		public String generate() throws IOException {
			StringWriter command = new StringWriter();
			PrintWriter printer = new PrintWriter(command);
			DocumentableHttpServletRequest request = new DocumentableHttpServletRequest(
					this.result.getRequest());

			printer.print("'");
			writeAuthority(request, printer);
			writePathAndQueryString(request, printer);
			printer.print("'");

			writeOptionToIncludeHeadersInOutput(printer);
			writeHttpMethodIfNecessary(request, printer);
			writeHeaders(request, printer);

			if (request.isMultipartRequest()) {
				writeParts(request, printer);
			}

			writeContent(request, printer);

			return command.toString();
		}

		private void writeAuthority(DocumentableHttpServletRequest request,
				PrintWriter writer) {
			writer.print(String.format("%s://%s", request.getScheme(), request.getHost()));

			if (isNonStandardPort(request)) {
				writer.print(String.format(":%d", request.getPort()));
			}
		}

		private boolean isNonStandardPort(DocumentableHttpServletRequest request) {
			return (SCHEME_HTTP.equals(request.getScheme()) && request.getPort() != STANDARD_PORT_HTTP)
					|| (SCHEME_HTTPS.equals(request.getScheme()) && request.getPort() != STANDARD_PORT_HTTPS);
		}

		private void writePathAndQueryString(DocumentableHttpServletRequest request,
				PrintWriter writer) {
			if (StringUtils.hasText(request.getContextPath())) {
				writer.print(String.format(
						request.getContextPath().startsWith("/") ? "%s" : "/%s",
						request.getContextPath()));
			}

			writer.print(request.getRequestUriWithQueryString());
		}

		private void writeOptionToIncludeHeadersInOutput(PrintWriter writer) {
			writer.print(" -i");
		}

		private void writeHttpMethodIfNecessary(DocumentableHttpServletRequest request,
				PrintWriter writer) {
			if (!request.isGetRequest()) {
				writer.print(String.format(" -X %s", request.getMethod()));
			}
		}

		private void writeHeaders(DocumentableHttpServletRequest request,
				PrintWriter writer) {
			for (Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
				for (String header : entry.getValue()) {
					writer.print(String.format(" -H '%s: %s'", entry.getKey(), header));
				}
			}
		}

		private void writeParts(DocumentableHttpServletRequest request, PrintWriter writer)
				throws IOException {
			for (Entry<String, List<MultipartFile>> entry : request.getMultipartFiles()
					.entrySet()) {
				for (MultipartFile file : entry.getValue()) {
					writer.printf(" -F '%s=", file.getName());
					if (!StringUtils.hasText(file.getOriginalFilename())) {
						writer.append(new String(file.getBytes()));
					}
					else {
						writer.printf("@%s", file.getOriginalFilename());
					}

					if (StringUtils.hasText(file.getContentType())) {
						writer.append(";type=").append(file.getContentType());
					}
					writer.append("'");
				}
			}

		}

		private void writeContent(DocumentableHttpServletRequest request,
				PrintWriter writer) throws IOException {
			if (request.getContentLength() > 0) {
				writer.print(String.format(" -d '%s'", request.getContentAsString()));
			}
			else if (request.isMultipartRequest()) {
				for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
					for (String value : entry.getValue()) {
						writer.print(String.format(" -F '%s=%s'", entry.getKey(), value));
					}
				}
			}
			else if (request.isPostRequest() || request.isPutRequest()) {
				String queryString = request.getParameterMapAsQueryString();
				if (StringUtils.hasText(queryString)) {
					writer.print(String.format(" -d '%s'", queryString));
				}
			}
		}
	}

}
