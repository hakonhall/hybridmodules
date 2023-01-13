all: m-2.jar

m-2.jar:
	javac -d target/classes `find src -name '*.java'`
	jar -c -f $@ --module-version 2 -C target/classes .

clean:
	rm -rf m-2.jar target
