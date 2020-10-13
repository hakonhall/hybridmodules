LIBS := mods/junit-jupiter-api-5.6.2.jar
MODULE_PATH_TEST_WO_DRIVER := target-test/one.jar:mods/junit-jupiter-api-5.6.2.jar:mods/junit-platform-commons-1.6.2.jar:mods/apiguardian-api-1.1.0.jar:mods/opentest4j-1.2.0.jar:mods/junit-platform-launcher-1.6.2.jar:mods/junit-platform-engine-1.6.2.jar
MODULE_PATH_TEST := mods/junit-jupiter-api-5.6.2.jar:mods/junit-platform-commons-1.6.2.jar:mods/apiguardian-api-1.1.0.jar:mods/opentest4j-1.2.0.jar:mods/junit-platform-launcher-1.6.2.jar:mods/junit-platform-engine-1.6.2.jar:lib/junit-jupiter-engine-5.6.2.jar

unit-tests: compile-test
	javahms -p $(MODULE_PATH_TEST) -p target-test/one.jar -m \
	    one/no.ion.jhms.junit.jupiter.driver.Main

compile-src:
	rm -rf target/classes
	mkdir -p target
	cp module-info-src.java src/module-info.java
	modulec src

compile-test: compile-src lib/junit-jupiter-engine-5.6.2.jar
	rm -rf target-test/classes
	mkdir -p target-test
	cp -r target/classes target-test/classes

	rm src/module-info.java
	rm -f test/module-info.java
	ln -s ../module-info-test.java test/module-info.java
	modulec -p $(MODULE_PATH_TEST) -o target-test test

modularize:
	org.junit.jupiter.api@5.6.2 org.junit.platform.commons@


lib/junit-jupiter-engine-5.6.2.jar: mods/junit-jupiter-engine-5.6.2.jar lib
	cp $< $@
	modularize-jar -u --module-version 5.6.2 -I junit-jupiter-engine/module-info.java -f lib/junit-jupiter-engine-5.6.2.jar -p mods/apiguardian-api-1.1.0.jar -p mods/junit-jupiter-api-5.6.2.jar -p mods/junit-platform-commons-1.6.2.jar -p mods/junit-platform-engine-1.6.2.jar -p mods/opentest4j-1.2.0.jar

lib:
	mkdir $@

clean:
	rm -rf lib src/module-info.java test/module-info.java target target-test
