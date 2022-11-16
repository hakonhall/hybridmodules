
subdirs = tests
.PHONY: all install clean $(subdirs)

target =
all: javahms $(subdirs)

javahms: javahms.sh
	rm -f $@
	# If the JAR is a prerequisite, then clean will fail on the second run
	cat $< ../no.ion.jhms/target/no.ion.jhms-3.0.0.jar > $@
	chmod +x $@

install: ~/bin/javahms

~/bin/javahms: javahms
	rm -f $@
	cp $< $@

$(subdirs):
	$(MAKE) -C $@ $(target)

clean: target = clean
clean: all
	rm -f javahms
