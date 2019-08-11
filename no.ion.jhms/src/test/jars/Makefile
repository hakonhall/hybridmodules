arguments :=
dirs := with-main-class required rich-descriptor

.PHONY: all
all: $(dirs)

.PHONY: $(dirs)
$(dirs):
	$(MAKE) -C $@ $(arguments)

clean: arguments = clean
clean: $(dirs)
