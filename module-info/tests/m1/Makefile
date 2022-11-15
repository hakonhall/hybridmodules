all:
	modc -m -v 0.0.1 -f no.mA.jar -s java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../module-info no.mA.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mA.jar
	rm -rf target

