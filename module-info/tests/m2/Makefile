all:
	modulec -p ../m1 -v 0.0.2 -f no.m2.jar java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../module-info no.m2.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.m2.jar
