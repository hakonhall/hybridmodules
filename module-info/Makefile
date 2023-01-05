# Phony targets are always built.
.PHONY: target/no.ion.moduleinfo-1.0.0.jar tests

all: target/module-info

target/module-info: bin/module-info.sh target/no.ion.moduleinfo-1.0.0.jar
	cat $^ > $@
	chmod +x $@

target/no.ion.moduleinfo-1.0.0.jar:
	mvn package


install: ~/bin/module-info

~/bin/module-info: target/module-info
	cp $< $@


tests:
	$(MAKE) -C $@


clean:
	$(MAKE) -C tests clean
	mvn clean
