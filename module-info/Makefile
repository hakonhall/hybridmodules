.PHONY: all build-jar clean install tests

all: module-info tests

module-info: module-info.sh no.ion.moduleinfo.jar
	cat $^ > $@
	chmod +x $@

no.ion.moduleinfo.jar:
	modc -m -o no.ion.moduleinfo.jar -e no.ion.Main -s java

install: ~/bin/module-info

~/bin/module-info: module-info
	rm -f $@
	cp $< $@

tests:
	$(MAKE) -C $@

clean:
	$(MAKE) -C tests clean
	rm -f no.ion.moduleinfo.jar
	rm -f module-info
	rm -rf target
