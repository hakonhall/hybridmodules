dirs = bundle diffcmd experiments jar javahms module-info no.ion.jhms \
 no.ion.jhms.bundle

.PHONY: all install clean doit $(dirs)

all: target =
all: doit

install: target = install
install: javahms

modc:
	type modc &> /dev/null || { echo "modc not in PATH, please install it from https://github.com/hakonhall/modulec"; exit 1; }

clean: target = clean
clean: doit

doit: $(dirs)
	@echo Success

bundle: no.ion.jhms.bundle modc
diffcmd:
experiments: modc
jar: diffcmd modc module-info
javahms: no.ion.jhms diffcmd modc
module-info: modc diffcmd
no.ion.jhms:
no.ion.jhms.bundle:

$(dirs):
	$(MAKE) -C $@ $(target)
