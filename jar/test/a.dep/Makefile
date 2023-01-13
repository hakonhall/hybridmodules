all:
	javac -d target/classes `find src -name '*.java'`
	jar -c -f target/a.dep-1.0.3.jar --module-version 1.0.3 -C target/classes .

clean:
	rm -rf target
