subdirs = helloworld test
target =

.PHONY: $(subdirs)

all: $(subdirs)
	../../../diffcmd/diffcmd ../../javahms -p helloworld:test -m test/com.test.TestApp

test: helloworld

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean:  $(subdirs)
