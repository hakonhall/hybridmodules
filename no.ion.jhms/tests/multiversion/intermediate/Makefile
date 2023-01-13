all: intermediate.jar

intermediate.jar:
	javac -d target/classes -p ../m-2 `find src -name '*.java'`
	jar -c -f target/$@ -C target/classes .

clean:
	rm -rf intermediate.jar target
