OUTPUT_DIR = mods
JAR = jars/commons-collections4-4.1.jar
MODULE_INFO = $(OUTPUT_DIR)/module-info.java
HYBRID_MODULE_JAR = no.ion.hybridmodules.modularizing.one@0.0.1.jar
HYBRID_MODULE_VERSION = 0.0.1

.PHONY: all
all:
	@echo 'Targets: module-info modular-jar package run'

# Example of modularization.
#
# This example hybrid module will depend on commons-collections, which is
# provided by Apache as an OSGi bundle. Our first step is to create a hybrid
# module commons-collections, which is done these steps:
#
#  1. Create a module-info.java blueprint using bundle.sh.
#  2. Manually verify and fix module-info.java.
#  2. Create a modular JAR using make-modular-jar.sh.
#
# Step 1. is done with:

.PHONY: module-info
module-info: $(OUTPUT_DIR)
	../../bundle/bundle.sh $(JAR) > $(MODULE_INFO)

# Step 2: Remove the @4.1.0 version from the header line of
# jars/commons-collections4-4.1-info.java. This version will be used in step 3.

VERSION = 4.1.0
MODULAR_JAR = $(OUTPUT_DIR)/org.apache.commons.collections4@$(VERSION).jar

# Step 3: Update the JAR file to a modular JAR with:

.PHONY: modular-jar
modular-jar: $(OUTPUT_DIR)
	cp $(JAR) $(MODULAR_JAR)
	../../sh/make-modular-jar.sh -u -f $(MODULAR_JAR) --module-info $(MODULE_INFO) --module-version $(VERSION)

$(OUTPUT_DIR):
	mkdir $@

.PHONY: package
package:
	@mkdir -p classes
	$(JAVA_HOME)/bin/javac -d classes -p $(MODULAR_JAR) $$(find src/main/java -name '*.java')
	$(JAVA_HOME)/bin/jar -c -f $(HYBRID_MODULE_JAR) --module-version $(HYBRID_MODULE_VERSION) -C classes .

.PHONY: run
run:
	$(JAVA_HOME)/bin/java -jar ../../no.ion.hybridmodules/target/no.ion.hybridmodules-0.1.0.jar --hybrid-module no.ion.hybridmodules.modularizing.one/no.ion.hybridmodules.modularizing.one.Main -p $(HYBRID_MODULE_JAR):$(MODULAR_JAR) -- this should print 4

.PHONY: clean
clean:
	@rm -vrf classes
	@rm -vf $(HYBRID_MODULE_JAR)

.PHONY: deepclean
deepclean: clean
	@rm -vf $(MODULE_INFO) $(MODULAR_JAR)
	@if test -d $(OUTPUT_DIR); then rmdir --ignore-fail-on-non-empty -v $(OUTPUT_DIR); fi
