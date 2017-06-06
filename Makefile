all:
	mkdir -p build
	javac -d build src/java/*java
run_server:
	java -classpath ./build main.ChatDotServer 4444 200
run_client:
	java -classpath ./build main.ChatDotClient bross password localhost 4444
run_client_gui:
	java -classpath ./build main.ChatDotClientInterface
test:
	java test.TestAll
clean:
	rm -rf build/*
