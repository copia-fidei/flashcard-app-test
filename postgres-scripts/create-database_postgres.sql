CREATE DATABASE collections;

\c collections

CREATE TABLE collections(
	name TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE flashcards(
	collection TEXT NOT NULL REFERENCES collections(name),
	front TEXT NOT NULL,
	back TEXT NOT NULL
);
