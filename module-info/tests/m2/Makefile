all:
	modc -p ../m1 -m -v 0.0.2 -o no.mB.jar -s java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../target/module-info no.mB.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mB.jar
	rm -rf target
