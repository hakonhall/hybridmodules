all:
	../../../modulec/modulec -v 0.0.1 -f no.m1.jar java
	../../../diffcmd/diffcmd -n ../javac-version-normalizer \
		../../module-info no.m1.jar

clean:
	../../../diffcmd/diffcmd --clean
	rm -f no.m1.jar
