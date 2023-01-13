.PHONY: test.jar

all: test.jar

test.jar:
	javac -d target/classes -p ../helloworld `find classes -name '*.java'`
	jar -c -f $@ -C target/classes .

clean:
	rm -rf test.jar target
