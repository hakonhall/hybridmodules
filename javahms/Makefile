
subdirs = tests
.PHONY: all install clean $(subdirs)

target =
all: $(subdirs)

install: ~/bin/javahms

~/bin/javahms: javahms
	rm -f $@
	ln -s $$PWD/$< $@

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: all
