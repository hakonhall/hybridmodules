.PHONY: all
all: one two

.PHONY: one
one:  ../../no.ion.hybridmodules/src/test/resources
	$(MAKE) -C $@
	cp one/lib/find.hybrid.module.one-1.2.3.jar ../../no.ion.hybridmodules/src/test/resources

.PHONY: two
two:  ../../no.ion.hybridmodules/src/test/resources
	$(MAKE) -C $@
	cp two/lib/find.hybrid.module.two-1.2.3.jar ../../no.ion.hybridmodules/src/test/resources

../../no.ion.hybridmodules/src/test/resources:
	mkdir -p ../../no.ion.hybridmodules/src/test/resources

re: clean all

clean:
	$(MAKE) -C one clean
	$(MAKE) -C two clean
