all:
	javac -d target/classes -p ../m1 `find java -name '*.java'`
	jar -c -f no.mB.jar --module-version 0.0.2 -C target/classes .
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../target/module-info no.mB.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mB.jar
	rm -rf target
