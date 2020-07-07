jarfile := junit-platform-console-standalone-1.6.2.jar

.PHONY: test

test: mods/$(jarfile) target-test/one.jar
	javahms -p mods/$(jarfile):target-test/one.jar \
                -m org.junit.platform.console.standalone/org.junit.platform.console.ConsoleLauncher \
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
