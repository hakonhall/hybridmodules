all: jar/case3-0.0.1.jar
	java -jar ../../../target/no.ion.hybridmodules-0.1.0.jar -p ../one/jar:jar -m no.case3/no.Main
	java -p ../one/jar:jar -m no.case3/no.Main

jar/case3-0.0.1.jar: src/module-info.java src/no/Main.java src/one/One3.java
	javac -d out -p ../one/jar $^
	mkdir -p jar
	jar -c -f $@ --module-version 0.0.1 -C out .

clean:
	rm -rf jar out
