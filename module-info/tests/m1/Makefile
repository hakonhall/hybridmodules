all:
	modc -m -v 0.0.1 -o no.mA.jar -s java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../target/module-info no.mA.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mA.jar
	rm -rf target
