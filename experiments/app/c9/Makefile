target := all

.PHONY: all
all: makesubdirs

.PHONY: clean
clean: target = clean
clean: makesubdirs

.PHONY: makesubdirs
makesubdirs:
	$(MAKE) -C A $(target)
	$(MAKE) -C 0 $(target)
