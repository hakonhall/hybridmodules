subdirs = tests
target =

.PHONY: all install clean $(subdirs)

all: $(subdirs)

install: ~/bin/modulec

~/bin/modulec: modulec
	rm -f $@
	ln -s $$PWD/$< $@

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: all
