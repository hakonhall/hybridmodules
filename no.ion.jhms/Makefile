.PHONY: all install clean tests build-jar

#   [all]     Package JAR, build javahms, and run tests.
all: build-jar target/javahms tests

build-jar target/no.ion.jhms-3.1.jar:
	mvn -nsu package

target/javahms: bin/javahms.sh target/no.ion.jhms-3.1.jar
	cat $^ > $@
	chmod +x $@

tests: target/javahms
	$(MAKE) -C tests


#   install   Package JAR and build javahms if necessary, and install javahms to
#             ~/bin.
install: ~/bin/javahms

~/bin/javahms: target/javahms
	cp $^ $@

#   clean
clean:
	$(MAKE) -C tests clean
	mvn -nsu clean
