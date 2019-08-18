.PHONY: tests

all: build-jar tests

build-jar:
	../modulec/modulec -e no.ion.Main java

tests:
	$(MAKE) -C $@

clean:
	$(MAKE) -C tests clean
	rm -f no.ion.moduleinfo.jar
