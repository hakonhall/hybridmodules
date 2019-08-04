.PHONY: m1 m2

all: build-jar m1 m2

build-jar:
	../../modulec -e no.ion.Main java

m1 m2:
	$(MAKE) -C $@

clean:
	$(MAKE) -C m2 clean
	$(MAKE) -C m1 clean
	rm -f no.ion.moduleinfo.jar
