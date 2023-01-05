.PHONY: m1

target =

all: m1
	../../../diffcmd/diffcmd ../../target/javahms -p m1 -m m1 x y

m1:
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: m1
	../../../diffcmd/diffcmd --clean
