all:
	mkdir -p build
	javac -d build src/java/*java
run_server:
	java -classpath ./build main.ChatDotServer 4444 200
run_client:
	java -classpath ./build main.ChatDotClient bross localhost 4444
test:
	java test.TestAll
clean:
	rm -rf build/*
