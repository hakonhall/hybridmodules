all:
	javac -d target/classes -cp ../a.dep/target/a.dep-1.0.3.jar src/a/foo/Foo.java
	jar -c -f target/a.foo.jar -C target/classes .

clean:
	rm -rf target
