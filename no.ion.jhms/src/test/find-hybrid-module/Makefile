.PHONY: all
all: one two

.PHONY: one
one:  ../resources
	$(MAKE) -C $@
	cp one/lib/find.hybrid.module.one-1.2.3.jar ../resources

.PHONY: two
two:  ../resources
	$(MAKE) -C $@
	cp two/lib/find.hybrid.module.two-1.2.3.jar ../resources

../resources:
	mkdir -p ../resources

re: clean all

clean:
	$(MAKE) -C one clean
	$(MAKE) -C two clean
