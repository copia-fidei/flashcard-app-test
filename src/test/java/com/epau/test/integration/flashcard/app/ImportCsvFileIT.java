package com.epau.test.integration.flashcard.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.readString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class ImportCsvFileIT extends Base {

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
		return "Dictionary.csv";
	}

	final Path RESOURCES = Path.of("src/test/resources");

	private String collectionDoesNotExistResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("collection-does-not-exist_Dictionary.csv.html"));
	}

	private String successfulDeletionResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("successful-deletion-of_Dictionary.csv.html"));
	}

	private String duplicateImportResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("duplicate-import-of_Dictionary.csv.html"));
	}

	private String collectionPageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("Dictionary.csv.html"));
	}

	private String successfulImportResponsePageReferenceFile() throws IOException {
		return readString(RESOURCES.resolve("successful-import-of_Dictionary.csv.html"));
	}

	private void verifyImportInDatabase() {
		var request = connection.request("SELECT front, back FROM flashcards WHERE collection = ?").parameters(getExampleCollection()).build();
		request.assertThat()
			   .column("front")
			   .value().isEqualTo("Accommodable")
			   .value().isEqualTo("Africander")
			   .value().isEqualTo("Background")
			   .value().isEqualTo("Farming")
			   .value().isEqualTo("Fishery")
			   .value().isEqualTo("Foreboder")
			   .value().isEqualTo("Full")
			   .value().isEqualTo("Graduator")
			   .value().isEqualTo("Help")
			   .value().isEqualTo("Hood");


		request.assertThat()
			   .column("back")
			   .value().isEqualTo("That may be accommodated, fitted, or made to agree.")
			   .value().isEqualTo("One born in Africa, the offspring of a white father and a \"colored\" mother. Also, and now commonly in Southern Africa, a native born of European settlers.")
			   .value().isEqualTo("A place in obscurity or retirement, or out of sight.")
			   .value().isEqualTo("of Farm")
			   .value().isEqualTo("The business or practice of catching fish; fishing.")
			   .value().isEqualTo("One who forebodes.")
			   .value().isEqualTo("Complete measure; utmost extent; the highest state or degree.")
			   .value().isEqualTo("An apparatus for diffusing a solution, as brine or vinegar, over a large surface, for exposure to the air.")
			   .value().isEqualTo("Remedy; relief; as, there is no help for it.")
			   .value().isEqualTo("A like appendage to a cloak or loose overcoat, that may be drawn up over the head at pleasure.");
	}
}
