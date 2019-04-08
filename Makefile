.DEFAULT_GOAL := build

.PHONY: help

build:
	./gradlew build -x test

upload:
	./gradlew upload -x test

help:
	@echo "Usage:"

