package com.epau.test.integration.flashcard.app;

import io.restassured.response.Response;
import org.assertj.db.type.AssertDbConnection;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.db.type.AssertDbConnectionFactory.of;

abstract class Base {

	// @formatter:off
	protected final AssertDbConnection connection = of(
			"jdbc:postgresql://localhost:15432/collections",
			"flashcards",
			"flashcards")
	.create();
	// @formatter:on


	protected final Path RESOURCES_DIR = Path.of("src/test/resources");

	protected abstract String getExampleCollection();

	protected String encode(String collection) {
		try {
			return new URI(null, null, collection, null).toASCIIString();
		} catch (URISyntaxException notExpected) {
			return null;
		}
	}

	protected long getAmountOfCollections() {
		return (long) connection.request("SELECT COUNT(name) FROM collections").build().getRow(0).getColumnValue(0).getValue();
	}

	protected Response importExampleCollection() {
		return Server.request().multiPart(RESOURCES_DIR.resolve(getExampleCollection()).toFile()).post("/flashcards/collections");
	}

	protected Response deleteExampleCollection() {
		return Server.request().delete("/flashcards/collections/" + encode(getExampleCollection()));
	}

	protected Response visitCollectionPage() {
		return Server.request().when().get("/flashcards/collections/" + encode(getExampleCollection()));
	}

	protected void verifyDeletionInDatabase() {
		var request = connection.request("SELECT front, back FROM flashcards WHERE collection = ?").parameters(getExampleCollection()).build();
		request.assertThat().column("front").isEmpty();
		request.assertThat().column("back").isEmpty();
	}
}

