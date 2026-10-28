package com.epau.test.integration.flashcard.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.nio.file.Files.readString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class ImportApkgFileIT extends Base {

	@Test
	void importFile() throws IOException {
		deleteExampleCollection();

		long amountOfCollectionsAtStart = getAmountOfCollections();

		importExampleCollection().then().assertThat().body(equalTo(successfulImportResponsePageReferenceFile()));
		importExampleCollection().then().assertThat().body(equalTo(duplicateImportResponsePageReferenceFile()));

		long amountOfCollectionsAfterImport = getAmountOfCollections();
		assertEquals(amountOfCollectionsAfterImport, amountOfCollectionsAtStart + 1);

		verifyImportInDatabase();
		visitCollectionPage().then().assertThat().body(equalTo(collectionPageReferenceFile()));

		deleteExampleCollection().then().assertThat().body(equalTo(successfulDeletionResponsePageReferenceFile()));
		verifyDeletionInDatabase();
		long amountOfCollectionsAfterDeletion = getAmountOfCollections();
		assertEquals(amountOfCollectionsAfterDeletion, amountOfCollectionsAtStart);
		deleteExampleCollection().then().assertThat().body(equalTo(collectionDoesNotExistResponsePageReferenceFile()));
		assertEquals(amountOfCollectionsAfterDeletion, amountOfCollectionsAtStart);
	}

	@Override
	protected String getExampleCollection() {
		return "Planets.apkg";
	}

	private String collectionDoesNotExistResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES_DIR.resolve("collection-does-not-exist_Planets.apkg.html"));
	}

	private String successfulDeletionResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES_DIR.resolve("successful-deletion-of_Planets.apkg.html"));
	}

	private String duplicateImportResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES_DIR.resolve("duplicate-import-of_Planets.apkg.html"));
	}

	private String collectionPageReferenceFile() throws IOException {
		return readString(RESOURCES_DIR.resolve("Planets.apkg.html"));
	}

	private String successfulImportResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES_DIR.resolve("successful-import-of_Planets.apkg.html"));
	}

	// @formatter:off
	private void verifyImportInDatabase() {
		var request = connection.request("SELECT front, back FROM flashcards WHERE collection = ?").parameters(getExampleCollection()).build();
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
	// @formatter:on
}
