dirs = bundle command-test experiments jar javahms modulec no.ion.jhms \
 no.ion.jhms.bundle

.PHONY: $(dirs)

target =
all: $(dirs)

clean: target = clean
clean: all

bundle: no.ion.jhms.bundle modulec
experiments: modulec
jar:
javahms: no.ion.jhms command-test
modulec:
no.ion.jhms:
no.ion.jhms.bundle:

$(dirs):
	$(MAKE) -C $@ $(target)
