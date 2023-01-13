all:
	javac -d target/classes -p ../m1:../m2 `find java -name '*.java'`
	jar -c -f no.mC.jar --module-version 0.0.2 -C target/classes .
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../target/module-info no.mC.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mC.jar
	rm -rf target
