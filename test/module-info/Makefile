.PHONY: m1

all: build-jar m1

build-jar:
	../../modulec -e no.ion.Main java

m1:
	$(MAKE) -C $@

clean:
	$(MAKE) -C m1 clean
	rm -f no.ion.moduleinfo.jar
