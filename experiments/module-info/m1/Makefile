version := $(shell javac --version | grep -Eo '[0-9.]+')

all:
	../../../modulec/modulec -v 0.0.1 -f no.m1.jar java
	../moduleinfo no.m1.jar no.m1 > out
	sed "s/JAVAC_VERSION/${version}/g" expected_out_template > expected_out
	diff -Nau expected_out out
	@echo Success

clean:
	rm -f expected_out out no.m1.jar
