JDK_HOME = ~/share/jdk-10.0.1
NAME = hybrid-modules
VERSION = 0.0.1

JAVAC = $(JDK_HOME)/bin/javac
JAR = $(JDK_HOME)/bin/jar
JAVA = $(JDK_HOME)/bin/java
JAR_FILENAME = $(NAME)-$(VERSION).jar

all: classes lib
	$(JAVAC) -d classes $(shell find src -name '*.java')
	$(JAR) -c -f lib/$(JAR_FILENAME) -e Main \
            -C classes .
	$(JAVA) -cp lib/$(JAR_FILENAME) TestMain

classes:
	mkdir classes

lib:
	mkdir lib

re: clean all

clean:
	rm -vrf classes lib
