all: intermediate.jar

intermediate.jar:
	javac -d target/classes -p ../m-2 `find src -name '*.java'`
	jar -c -f $@ -C target/classes .

clean:
	rm -f intermediate.jar
	rm -rf target
