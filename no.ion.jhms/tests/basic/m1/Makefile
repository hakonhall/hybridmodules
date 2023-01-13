all: m1.jar

m1.jar:
	javac -d target/classes `find src -name '*.java'`
	jar -c -f $@ -e m1.Main -C target/classes .

clean:
	rm -rf m1.jar target
