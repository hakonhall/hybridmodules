all:
	../../../sh/modulec -v 0.0.1 -f no.m1.jar java
	../moduleinfo no.m1.jar no.m1 > out
	v=$(shell javac --version)
	sed "s/JAVAC_VERSION/$(v)/g" expected_out_template > expected_out
	diff -Nau expected_out out

clean:
	rm -f no.m1.jar
