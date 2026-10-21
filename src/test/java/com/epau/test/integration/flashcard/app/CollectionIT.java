package com.epau.test.integration.flashcard.app;

import io.restassured.response.Response;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static java.nio.file.Files.readString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class CollectionIT {
	
	private AssertDbConnection newConnection() {
		return AssertDbConnectionFactory.of("jdbc:postgresql://localhost:15432/collections", "flashcards", "flashcards").create();
	}
	
	@Test
	void import_APKG_file() throws IOException, URISyntaxException {
		var connection = newConnection();
		
		deleteExampleCollection();
		
		long amountOfCollectionsAtStart = getCollectionCount(connection);
		
		importExampleCollection().then().assertThat().body(equalTo(successfulImportResponsePageReferenceFile()));
		importExampleCollection().then().assertThat().body(equalTo(duplicateImportResponsePageReferenceFile()));

		long amountOfCollectionsAfterImport = getCollectionCount(connection);
		assertEquals(amountOfCollectionsAfterImport, amountOfCollectionsAtStart + 1);
		
		verifyImportInDatabase(connection);
		visitCollectionPage().then().assertThat().body(equalTo(collectionPageReferenceFile()));

		deleteExampleCollection().then().assertThat().body(equalTo(successfulDeletionResponsePageReferenceFile()));
		verifyDeletionInDatabase(connection);
		long amountOfCollectionsAfterDeletion = getCollectionCount(connection);
		assertEquals(amountOfCollectionsAfterDeletion, amountOfCollectionsAtStart);
		deleteExampleCollection().then().assertThat().body(equalTo(collectionDoesNotExistResponsePageReferenceFile()));
		assertEquals(amountOfCollectionsAfterDeletion, amountOfCollectionsAtStart);
	}

	final String EXAMPLE_COLLECTION = "Planets_Of_Our_Solar_System_Plus_Orbit__Name_Origin.apkg";

	final Path RESOURCES = Path.of("src/test/resources");

	private String collectionDoesNotExistResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("collection-does-not-exist.html"));
	}

	private String successfulDeletionResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("successful-deletion-of_Planets_Of_Our_Solar_System_Plus_Orbit__Name_Origin.apkg.html"));
	}

	private String duplicateImportResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("duplicate-import-of_Planets_Of_Our_Solar_System_Plus_Orbit__Name_Origin.apkg.html"));
	}

	private String collectionPageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("Planets_Of_Our_Solar_System_Plus_Orbit__Name_Origin.apkg.html"));
	}

	private Response visitCollectionPage() throws URISyntaxException {
		return Server.request().when().get("/flashcards/collections/" + encode(EXAMPLE_COLLECTION));
	}

	private String successfulImportResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("successful-import-of_Planets_Of_Our_Solar_System_Plus_Orbit__Name_Origin.apkg.html"));
	}

	private void verifyImportInDatabase(AssertDbConnection connection) {
		var request = connection.request("SELECT front, back FROM flashcards WHERE collection = ?").parameters(EXAMPLE_COLLECTION).build();
		request.assertThat()
			.column("front")
				.value().isEqualTo("Mercury")
				.value().isEqualTo("Venus")
				.value().isEqualTo("Earth")
				.value().isEqualTo("Mars")
				.value().isEqualTo("Jupiter")
				.value().isEqualTo("Saturn")
				.value().isEqualTo("Uranus")
				.value().isEqualTo("Neptune");

		request.assertThat()
			.column("back")
				.value().isEqualTo("1st")
				.value().isEqualTo("2nd")
				.value().isEqualTo("3rd")
				.value().isEqualTo("4th")
				.value().isEqualTo("5th")
				.value().isEqualTo("6th")
				.value().isEqualTo("7th")
				.value().isEqualTo("8th");
	}

	private void verifyDeletionInDatabase(AssertDbConnection connection) {
		var request = connection.request("SELECT front, back FROM flashcards WHERE collection = ?").parameters(EXAMPLE_COLLECTION).build();
		request.assertThat().column("front").isEmpty();
		request.assertThat().column("back").isEmpty();
	}

	private Response importExampleCollection() {
		return Server.request().multiPart(RESOURCES.resolve(EXAMPLE_COLLECTION).toFile()).post("/flashcards/collections");
	}

	private Response deleteExampleCollection() throws URISyntaxException {
		return Server.request().delete("/flashcards/collections/" + encode(EXAMPLE_COLLECTION));
	}

	private long getCollectionCount(AssertDbConnection connection) {
		return (long) connection.request("SELECT COUNT(name) FROM collections").build().getRow(0).getColumnValue(0).getValue();
	}

	private String encode(String collection) throws URISyntaxException {
		return new URI(null, null, collection, null).toASCIIString();
	}
}
