javaPaths := $(shell find java -name '*.java')
javaFiles := $(patsubst java/%,%,$(javaPaths))
classPaths := $(patsubst java/%.java,classes/%.class,$(javaPaths))
module := no.m2
jarPath := jar/$(module).jar

all: $(jarPath)
	java -p ../jar -m no.ion.mii $(jarPath) $(module)

$(jarPath): $(classPaths)
	mkdir -p jar
	jar -c -f $@ --module-version 0.0.2 -C classes .

$(classPaths): $(javaPaths)
	javac -p ../m1/jar -d classes $^

clean:
	rm -rf classes jar
