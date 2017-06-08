BUILD_OPT = -d build
CLASSPATH_OPT = -classpath 'build'
T_CLASSPATH_OPT = -classpath 'build:lib/*'
.PHONY: test

all:
	mkdir -p build
	javac $(CLASSPATH_OPT) $(BUILD_OPT) src/java/*java
	javac $(T_CLASSPATH_OPT) $(BUILD_OPT) test/*java
run_server:
	java $(CLASSPATH_OPT) main.ChatDotServer 4444 200
run_client:
	java $(CLASSPATH_OPT) main.ChatDotClient bross password localhost 4444
run_client_gui:
	java $(CLASSPATH_OPT) main.ChatDotClientInterface
test:
	java $(T_CLASSPATH_OPT) org.junit.runner.JUnitCore main.ChatDotServerTest
clean:
	rm -rf build/*
