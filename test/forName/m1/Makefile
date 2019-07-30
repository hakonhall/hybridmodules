m1jarPath := jar/no.ion.m1.jar
m2jarPath := ../m2/jar/no.ion.m2.jar

all: $(m1jarPath)
	java -p $(m2jarPath):$(m1jarPath) -m no.ion.m1
	java -cp $(m1jarPath):$(m2jarPath) no.ion.p1.Main
	java -jar  ~/github/hakonhall/hybridmodules/target/no.ion.jhms-2.0.0.jar -p $(m1jarPath):$(m2jarPath) -m no.ion.m1/no.ion.p1.Main

$(m1jarPath): classes/module-info.class classes/no/ion/p1/Main.class
	mkdir -p jar
	jar -c --module-version 0.0.2 -f $(m1jarPath) -e no.ion.p1.Main -C classes .

classes/%.class: java/%.java
	javac -d classes -p ../m2/jar/no.ion.m2.jar $^

clean:
	rm -rf classes
