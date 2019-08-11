subdirs = trivial
target =

.PHONY: $(subdirs)

all: $(subdirs)

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: all
