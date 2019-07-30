arguments :=
dirs := $(patsubst %/,%,$(wildcard */))

.PHONY: all
all: $(dirs)

.PHONY: $(dirs)
$(dirs):
	$(MAKE) -C $@ $(arguments)

clean: arguments = clean
clean: $(dirs)
