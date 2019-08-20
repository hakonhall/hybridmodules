subdirs = A B 0

.PHONY: all clean $(subdirs)

all: target =
all: $(subdirs)

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: $(subdirs)
