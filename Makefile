.PHONY: build test run run-client clean

build:
	./gradlew build

test: build

run:
	./gradlew runClient

run-client: run

clean:
	./gradlew clean
