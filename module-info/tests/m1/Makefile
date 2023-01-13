all:
	javac -d target/classes `find java -name '*.java'`
	jar -c -f no.mA.jar --module-version 0.0.1 -C target/classes .
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../target/module-info no.mA.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mA.jar
	rm -rf target
