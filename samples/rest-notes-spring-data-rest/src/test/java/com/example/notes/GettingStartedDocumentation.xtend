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
package com.example.notes

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import java.net.URI
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.hateoas.MediaTypes
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.springframework.restdocs.RestDocumentation.document
import static org.springframework.restdocs.RestDocumentation.documentationConfiguration
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@RunWith(typeof(SpringJUnit4ClassRunner))
@SpringApplicationConfiguration(classes=typeof(RestNotesSpringDataRest))
@WebAppConfiguration
class GettingStartedDocumentation {

	@Autowired ObjectMapper objectMapper

	@Autowired WebApplicationContext context

	MockMvc mockMvc

	private def URI asUri(String s) {
		URI.create(s)
	}

	@Before def void setUp() {
		this.mockMvc = MockMvcBuilders::webAppContextSetup(this.context) //
		.apply(documentationConfiguration()) //
		.alwaysDo(document("{method-name}/{step}/")) //
		.build()
	}

	@Test def void index() throws Exception {
		this.mockMvc //
		.perform(get("/") //
		.accept(MediaTypes::HAL_JSON)) //
		.andExpect(status().isOk()) //
		.andExpect(jsonPath("_links.notes", is(notNullValue()))) //
		.andExpect(jsonPath("_links.tags", is(notNullValue())))
	}

	@Test def void creatingANote() throws Exception {
		createNote => [ noteUri |
			noteUri.getNote => [ note |
				createTag => [ tagUri |
					tagUri.getTag
					tagUri.createTaggedNote => [ taggedNoteUri |
						taggedNoteUri.getNote => [ taggedNote |
							getLink(taggedNote, "tags").getTags
						]
					]
					tagExistingNote(noteUri, tagUri)
				]
				getLink(note, "tags").getTags
			]
		]
	}

	def URI createNote() throws Exception {
		this.mockMvc //
		.perform(post("/notes") //
		.contentType(MediaTypes::HAL_JSON) //
		.content(objectMapper.writeValueAsString(#{ //
			'title' -> 'Note creation with cURL', //
			'body' -> 'An example of how to create a note using cURL' //
		}))) //
		.andExpect(status().isCreated()) //
		.andExpect(header().string("Location", notNullValue())) //
		.andReturn() //
		.getResponse() //
		.getHeader("Location").asUri
	}

	def MvcResult getNote(URI noteLocation) throws Exception {
		this.mockMvc //
		.perform(get(noteLocation)) //
		.andExpect(status().isOk()) //
		.andExpect(jsonPath("title", is(notNullValue()))) //
		.andExpect(jsonPath("body", is(notNullValue()))) //
		.andExpect(jsonPath("_links.tags", is(notNullValue()))) //
		.andReturn()
	}

	def URI createTag() throws Exception {
		this.mockMvc.perform( //
		post("/tags") //
		.contentType(MediaTypes::HAL_JSON) //
		.content(objectMapper.writeValueAsString(#{
			"name" -> "getting-started"
		}))) //
		.andExpect(status().isCreated()) //
		.andExpect(header().string("Location", notNullValue())) //
		.andReturn() //
		.getResponse() //
		.getHeader("Location") //
		.asUri
	}

	def MvcResult getTag(URI tagLocation) throws Exception {
		this.mockMvc //
		.perform(get(tagLocation)) //
		.andExpect(status().isOk()) //
		.andExpect(jsonPath("name", is(notNullValue()))) //
		.andExpect(jsonPath("_links.notes", is(notNullValue()))) //
		.andReturn()
	}

	def URI createTaggedNote(URI tag) throws Exception {
		this.mockMvc //
		.perform(post("/notes") //
		.contentType(MediaTypes::HAL_JSON) //
		.content(objectMapper.writeValueAsString(#{
			'title' -> 'Tagged note creation with cURL',
			'body' -> 'An example of how to create a tagged note using cURL',
			'tags' -> #[tag]
		}))) //
		.andExpect(status().isCreated()) //
		.andExpect(header().string("Location", notNullValue())) //
		.andReturn() //
		.getResponse() //
		.getHeader("Location") //
		.asUri
	}

	def MvcResult getTags(URI noteTagsLocation) throws Exception {
		this.mockMvc //
		.perform(get(noteTagsLocation)) //
		.andExpect(status().isOk()) //
		.andExpect(jsonPath("_embedded.tags", sizeOf(1))) //
		.andReturn()
	}

	def sizeOf(int size) {
		return hasSize(size)
	}

	def void tagExistingNote(URI noteLocation, URI tagLocation) throws Exception {
		this.mockMvc.perform(patch(noteLocation) //
		.contentType(MediaTypes::HAL_JSON) //
		.content(objectMapper.writeValueAsString(#{
			'tags' -> #[tagLocation]
		}))) //
		.andExpect(status() //
		.isNoContent())
	}

	def MvcResult getTaggedExistingNote(URI noteLocation) throws Exception {
		return this.mockMvc //
		.perform(get(noteLocation)) //
		.andExpect(status().isOk()) //
		.andReturn()
	}

	def MvcResult getTagsForExistingNote(URI noteTagsLocation) throws Exception {
		this.mockMvc //
		.perform(get(noteTagsLocation)) //
		.andExpect(status().isOk()) //
		.andExpect(jsonPath("_embedded.tags", sizeOf(1)))
		.andReturn
	}

	def URI getLink(MvcResult result, String rel) throws Exception {
		JsonPath::parse(result.getResponse() //
		.getContentAsString()) //
		.read('''_links.«rel».href'''.toString).asUri
	}

}
