all:
	javac -d build src/java/*
test:
	java test.TestAll
clean:
	rm -rf build/*
