.PHONY: all build-jar clean install tests out/no.ion.moduleinfo@0.1.0.jar

all: module-info tests

module-info: module-info.sh out/no.ion.moduleinfo@0.1.0.jar
	cat $^ > $@
	chmod +x $@

out/no.ion.moduleinfo@0.1.0.jar:
	modco -v 0.1.0 -s java -e .Main

no.ion.moduleinfo.jar:
	modc -m -o no.ion.moduleinfo.jar -e no.ion.moduleinfo.Main -s java

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
	rm -rf target out
