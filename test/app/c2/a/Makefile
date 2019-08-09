javaPaths := $(shell find java -name '*.java')
javaFiles := $(patsubst java/%,%,$(javaPaths))
classPaths := $(patsubst java/%.java,classes/%.class,$(javaPaths))
jarPath := jar/out.jar

all: $(jarPath)

$(jarPath): $(classPaths)
	mkdir -p jar
	jar -c -f $@ --module-version 1.0.0 -C classes .

$(classPaths): $(javaPaths)
	javac -d classes -p ../b1/jar:../b2/jar $^

clean:
	rm -rf classes jar

cleanall: clean
	@echo "Run compile.sh to recreate Makefile"
	rm -f Makefile
