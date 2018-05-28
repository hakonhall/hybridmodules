JDK_HOME = ~/share/jdk-10.0.1
JAVAC = $(JDK_HOME)/bin/javac
JAR = $(JDK_HOME)/bin/jar

VERSION = 1.2.3
JAR_FILE = lib/find.hybrid.module.one-$(VERSION).jar

.PHONY: all

all: classes lib
	$(JAVAC) -d classes $(shell find src -name '*.java')
	$(JAR) -c -f $(JAR_FILE) --module-version $(VERSION) -C classes .

classes lib:
	mkdir $@

re: clean all

clean:
	rm -vrf classes lib
