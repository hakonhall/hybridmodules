modulejarfile := junit-platform-console-standalone-1.6.2.jar
barejarfile := org.junit.platform.console.standalone-1.6.2.jar
jarfile := $(barejarfile)

.PHONY: test

test: mods/$(jarfile) target-test/one.jar
	java --add-modules ALL-MODULE-PATH \
             --module-path mods/$(jarfile):target-test/one.jar \
             --module org.junit.platform.console.standalone/org.junit.platform.console.ConsoleLauncher \
             -o one

target-test/one.jar: mods/$(jarfile)
	cp module-info-src.java src/module-info.java
	modulec src

	rm -rf target-test/classes
	mkdir -p target-test
	cp -r target/classes target-test/classes
	rm src/module-info.java
	cp module-info-test.java test/module-info.java
	modulec -p mods/$(jarfile) -o target-test test

mods/$(jarfile): mods
	cp ../junit-jupiter/target/$(jarfile) $@

mods:
	mkdir mods

clean:
	rm -rf mods src/module-info.java test/module-info.java target target-test
