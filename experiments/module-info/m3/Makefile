version := $(shell javac --version | grep -Eo '[0-9.]+')

all:
	../../../bin/modulec -p ../m1:../m2 -v 0.0.2 -f no.m3.jar java
	../moduleinfo no.m3.jar no.m3 > out
	sed "s/JAVAC_VERSION/${version}/g" expected_out_template > expected_out
	diff -Nau expected_out out
	@echo Success

clean:
	rm -f expected_out out no.m3.jar
