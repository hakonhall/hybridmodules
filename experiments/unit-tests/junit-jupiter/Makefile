f := junit-platform-console-standalone-1.6.2.jar
b := org.junit.platform.console.standalone-1.6.2.jar

all: target/$(f) target/$(b)

target/$(f): module-info.java
	rm -rf target
	mkdir target
	cp lib/$(f) target
	../../../jar/modularize-jar -u -f target/$(f) -I module-info.java \
            -V 1.6.2

target/$(b): lib/$(f)
	cp lib/$(f) $@
	zip -d $@ module-info.class

clean:
	rm -rf target
