.PHONY: m1

target =

all: m1
	../../../command-test/command-test ../../javahms -p m1 -m m1 x y

m1:
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: m1
	../../../command-test/command-test --clean
