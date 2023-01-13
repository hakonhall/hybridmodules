helloworld.jar:
	javac -d target/classes `find classes -name '*.java'`
	jar -c -f $@ -C target/classes .

clean:
	rm -rf helloworld.jar target
