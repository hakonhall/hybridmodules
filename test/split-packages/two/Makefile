all: jar/out.mod-0.0.1.jar

jar/out.mod-0.0.1.jar: src/module-info.java src/two/Two.java src/common/Internal.java
	javac -d out $^
	mkdir -p jar
	jar -c -f $@ --module-version 0.0.1 -C out .

clean:
	rm -rf jar out
