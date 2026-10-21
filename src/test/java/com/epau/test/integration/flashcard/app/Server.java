package com.epau.test.integration.flashcard.app;

import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public interface Server {

	int PORT = 18181;

	static RequestSpecification request() {
		return given().port(PORT);
	}
}
