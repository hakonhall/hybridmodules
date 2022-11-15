.PHONY: test.jar

all: test.jar

test.jar:
	modc -p ../helloworld -m -f $@ -s classes

clean:
	rm -rf test.jar target
