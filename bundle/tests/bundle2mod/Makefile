OUTPUT_DIR = mods
JAR = jars/commons-collections4-4.1.jar
MODULE_INFO = $(OUTPUT_DIR)/module-info.java
HYBRID_MODULE_JAR = no.ion.hybridmodules.modularizing.one@0.0.1.jar
HYBRID_MODULE_VERSION = 0.0.1

.PHONY: all
all:
	@echo 'Targets:'
	@echo '    Step 1. [module-info] - Make module-info.java from OSGi bundle'
	@echo '    Step 2. Note the version in mods/module-info.java'
	@echo '    Step 3. [modular-jar] - Make modular JAR w/version from above'
	@echo '    Step 4. [package] - Package test app depending on the modular JAR'
	@echo '    Step 5. [run] - Run test app using the modular JAR'

# Example of modularization.
#
# This example hybrid module will depend on commons-collections, which is
# provided by Apache as an OSGi bundle. Our first step is to create a hybrid
# module commons-collections, which is done these steps:
#
#  1. Create a module-info.java blueprint using bundle.sh.
#  2. Manually verify and fix module-info.java.
#  2. Create a modular JAR using modularize-jar.
#
# Step 1. is done with:

.PHONY: module-info
module-info: $(OUTPUT_DIR)
	../../../bundle/bundle.sh $(JAR) > $(MODULE_INFO)

# Step 2: Remove the @4.1.0 version from the header line of
# jars/commons-collections4-4.1-info.java. This version will be used in step 3
# (--module-version below).

VERSION = 4.1.0
MODULAR_JAR = $(OUTPUT_DIR)/org.apache.commons.collections4@$(VERSION).jar

# Step 3: Update the JAR file to a modular JAR with:

.PHONY: modular-jar
modular-jar: $(OUTPUT_DIR)
	cp $(JAR) $(MODULAR_JAR)
	../../../modularize-jar/modularize-jar -u -f $(MODULAR_JAR) --module-info $(MODULE_INFO) --module-version $(VERSION)

$(OUTPUT_DIR):
	mkdir $@

# Step 4. Verify the JAR is indeed modular by building a test module that
# requires it.

.PHONY: package
package:
	@mkdir -p classes
	$(JAVA_HOME)/bin/javac -d classes -p $(MODULAR_JAR) $$(find src/main/java -name '*.java')
	$(JAVA_HOME)/bin/jar -c -f $(HYBRID_MODULE_JAR) --module-version $(HYBRID_MODULE_VERSION) -C classes .

# Step 5. And run it with hybrid modules.

.PHONY: run
run:
	../../../javahms/javahms -p $(HYBRID_MODULE_JAR):$(MODULAR_JAR) -m no.ion.hybridmodules.modularizing.one/no.ion.hybridmodules.modularizing.one.Main this should print the number of arguments which is 10

.PHONY: clean
clean:
	@rm -vrf classes
	@rm -vf $(HYBRID_MODULE_JAR)

.PHONY: deepclean
deepclean: clean
	@rm -vf $(MODULE_INFO) $(MODULAR_JAR)
	@if test -d $(OUTPUT_DIR); then rmdir --ignore-fail-on-non-empty -v $(OUTPUT_DIR); fi
