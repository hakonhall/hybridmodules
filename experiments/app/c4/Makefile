target := all

.PHONY: all
all: makesubdirs
	java -p 0/jar:A/jar:B/jar:C/jar -m root

.PHONY: clean
clean: target = clean
clean: makesubdirs

.PHONY: makesubdirs
makesubdirs:
	$(MAKE) -C C $(target)
	$(MAKE) -C B $(target)
	$(MAKE) -C A $(target)
	$(MAKE) -C 0 $(target)
