subdirs = basic tutorial

.PHONY: $(subdirs)

target =

all: $(subdirs)

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: all
