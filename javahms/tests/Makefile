subdirs = basic

.PHONY: $(subdirs)

target =

all: basic

basic:
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: all
