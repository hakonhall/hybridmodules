all:
	modulec -p ../m1:../m2 -v 0.0.2 -f no.m3.jar java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../module-info no.m3.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.m3.jar
