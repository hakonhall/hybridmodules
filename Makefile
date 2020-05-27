dirs = bundle diffcmd experiments jar javahms module-info no.ion.jhms \
 no.ion.jhms.bundle

.PHONY: all modulec install clean doit $(dirs)

all: target =
all: doit

install: target = install
install: modulec javahms

modulec:
	type modulec &> /dev/null || { echo "modulec not in PATH, please install it from https://github.com/hakonhall/modulec"; exit 1; }

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
