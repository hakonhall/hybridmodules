dirs = bundle diffcmd experiments jar javahms modulec no.ion.jhms \
 no.ion.jhms.bundle

.PHONY: $(dirs)

target =
all: $(dirs)
	@echo Success

clean: target = clean
clean: all

bundle: no.ion.jhms.bundle modulec
diffcmd:
experiments: modulec
jar:
javahms: no.ion.jhms diffcmd modulec
modulec: diffcmd
no.ion.jhms:
no.ion.jhms.bundle:

$(dirs):
	$(MAKE) -C $@ $(target)
