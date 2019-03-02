javaPaths := $(shell find java -name '*.java')
javaFiles := $(patsubst java/%,%,$(javaPaths))
classPaths := $(patsubst java/%.java,classes/%.class,$(javaPaths))
jarPath := jar/no.ion.mii.jar

all: $(jarPath)

$(jarPath): $(classPaths)
	mkdir -p jar
	jar -c -f $@ --module-version 0.0.1 --main-class no.ion.Main -C classes .

$(classPaths): $(javaPaths)
	javac -d classes $^

clean:
	rm -rf classes jar
