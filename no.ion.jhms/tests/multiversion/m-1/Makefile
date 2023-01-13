all: m-1.jar

m-1.jar:
	javac -d target/classes `find src -name '*.java'`
	jar -c -f $@ --module-version 1 -C target/classes .

clean:
	rm -rf m-1.jar target
