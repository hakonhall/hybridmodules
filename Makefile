dirs = diffcmd jar module-info no.ion.jhms no.ion.jhms.bundle

.PHONY: all install clean doit $(dirs)

all: target =
all: doit

install: target = install
install: no.ion.jhms no.ion.jhms.bundle

clean: target = clean
clean: doit

doit: $(dirs)
	@echo Success

diffcmd:
jar: diffcmd module-info
module-info: diffcmd
no.ion.jhms.bundle:
no.ion.jhms: diffcmd

$(dirs):
	$(MAKE) -C $@ $(target)
