all: jar/case1-0.0.1.jar
	java -jar ../../../target/no.ion.hybridmodules-0.1.0.jar -p ../one/jar:../two/jar:jar -m no.case1/no.Main

jar/case1-0.0.1.jar: src/module-info.java src/no/Main.java
	javac -d out -p ../one/jar:../two/jar $^
	mkdir -p jar
	jar -c -f $@ --module-version 0.0.1 -C out .

clean:
	rm -rf jar out
