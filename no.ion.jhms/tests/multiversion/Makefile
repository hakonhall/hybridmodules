subdirs = m-1 m-2 intermediate main
target =

.PHONY: $(subdirs)

# make clean all doesn't work...

all: $(subdirs)
	../../../diffcmd/diffcmd ../../target/javahms -p \
                m-1:m-2:intermediate/target:main/target -m main
	../../../diffcmd/diffcmd -d module-graph ../../target/javahms -p \
		m-1:m-2:intermediate/target:main/target \
		-g '' -m main

m-1:
m-2:
intermediate: m-1
main: m-1 intermediate

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean:  $(subdirs)
	../../../diffcmd/diffcmd --clean
	../../../diffcmd/diffcmd --clean -d module-graph
