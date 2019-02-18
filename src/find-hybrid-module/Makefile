.PHONY: all
all: one two

.PHONY: one
one:  ../../src/test/resources
	$(MAKE) -C $@
	cp one/lib/find.hybrid.module.one-1.2.3.jar ../../src/test/resources

.PHONY: two
two:  ../../src/test/resources
	$(MAKE) -C $@
	cp two/lib/find.hybrid.module.two-1.2.3.jar ../../src/test/resources

../../src/test/resources:
	mkdir -p ../../src/test/resources

re: clean all

clean:
	$(MAKE) -C one clean
	$(MAKE) -C two clean
