.PHONY: find-hybrid-module hybridmodules bundle
all: find-hybrid-module hybridmodules bundle

# Makes test hybrid modules in src/test/resources used during testing in
# src/test/jave
find-hybrid-module: src/test/resources/find.hybrid.module.one-1.2.3.jar \
	src/test/resources/find.hybrid.module.two-1.2.3.jar

src/test/resources/find.hybrid.module.one-1.2.3.jar: src/test/resources \
	src/test/find-hybrid-module/one/lib/find.hybrid.module.one-1.2.3.jar
	cp src/test/find-hybrid-module/one/lib/find.hybrid.module.one-1.2.3.jar $@

src/test/find-hybrid-module/one/lib/find.hybrid.module.one-1.2.3.jar:
	$(MAKE) -C src/test/find-hybrid-module/one

src/test/resources/find.hybrid.module.two-1.2.3.jar: src/test/resources \
	src/test/find-hybrid-module/two/lib/find.hybrid.module.two-1.2.3.jar
	cp src/test/find-hybrid-module/two/lib/find.hybrid.module.two-1.2.3.jar $@

src/test/find-hybrid-module/two/lib/find.hybrid.module.two-1.2.3.jar:
	$(MAKE) -C src/test/find-hybrid-module/two

src/test/resources:
	mkdir -p $@

hybridmodules:
	mvn -nsu install
	jar -u -f target/$(jarFilename) --main-class $(mainClass)


rootPath := src/main/java
destPath := classes
javaPaths := $(shell find $(rootPath) -name '*.java')
classFiles := $(patsubst $(rootPath)/%.java,%.class,$(javaPaths))
classPaths := $(patsubst $(rootPath)/%.java,$(destPath)/%.class,$(javaPaths))
module := no.ion.jhms
version := 2.0.0
jarFilename := $(module)-$(version).jar
jarPath := jar/$(jarFilename)
launcherName := jhms
launcherPath := $(launcherName)/bin/$(launcherName)
mainClass := no.ion.jhms.Main

.PHONY: jhms
jhms: $(jarPath)

$(jarPath): jar $(classPaths)
	jar -c -f $@ -e $(mainClass) -C $(destPath) .
	# This does not good since it doesn't include all platform modules.
	#jlink -p jar --launcher $(launcherName)=no.ion.jhms --output $(launcherName) --add-modules no.ion.jhms

jar:
	mkdir jar

$(classPaths): $(javaPaths)
	javac -d $(destPath) $^



bundle:
	$(MAKE) -C $@

clean:
	$(MAKE) -C bundle clean
	mvn -nsu clean
	rm -f src/test/resources/find.hybrid.module.one-1.2.3.jar
	rm -f src/test/resources/find.hybrid.module.two-1.2.3.jar
	if test -d src/test/resources; then rmdir src/test/resources; fi
	$(MAKE) -C src/test/find-hybrid-module/one clean
	$(MAKE) -C src/test/find-hybrid-module/two clean

	rm -rf $(launcherName)
	rm -rf $(destPath) jar
