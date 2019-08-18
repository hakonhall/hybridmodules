dirs = bundle diffcmd experiments jar javahms modulec module-info no.ion.jhms \
 no.ion.jhms.bundle

.PHONY: all clean doit $(dirs)

all: target =
all: doit

clean: target = clean
clean: doit

doit: $(dirs)
	@echo Success

bundle: no.ion.jhms.bundle modulec
diffcmd:
experiments: modulec
jar:
javahms: no.ion.jhms diffcmd modulec
modulec: diffcmd
module-info: modulec diffcmd
no.ion.jhms:
no.ion.jhms.bundle:

$(dirs):
	$(MAKE) -C $@ $(target)
