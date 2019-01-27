.DEFAULT_GOAL := build

.PHONY: help

build:
	./gradlew build -x test

help:
	@echo "Usage:"

