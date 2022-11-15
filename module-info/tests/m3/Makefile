all:
	modc -p ../m1:../m2 -m -v 0.0.2 -f no.mC.jar -s java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../module-info no.mC.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.mC.jar
	rm -rf target
