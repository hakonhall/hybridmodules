.PHONY: test.jar

all: test.jar

test.jar:
	modc -p ../helloworld -m -o $@ -s classes

clean:
	rm -rf test.jar target
