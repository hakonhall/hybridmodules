test_directories = m1 m2 m3

.PHONY: $(test_directories)

all: build-jar $(test_directories)

build-jar:
	../../modulec/modulec -e no.ion.Main java

$(test_directories):
	$(MAKE) -C $@

clean:
	$(MAKE) -C m3 clean
	$(MAKE) -C m2 clean
	$(MAKE) -C m1 clean
	rm -f no.ion.moduleinfo.jar
