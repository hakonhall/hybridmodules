VERSION = 1.2.3
JAR_FILE = lib/find.hybrid.module.two-$(VERSION).jar

.PHONY: all

all: classes lib
	javac -d classes -p ../one/lib $(shell find src -name '*.java')
	jar -c -f $(JAR_FILE) --module-version $(VERSION) -C classes .

classes lib:
	mkdir $@

re: clean all

clean:
	rm -vrf classes lib
