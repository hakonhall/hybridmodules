
all: target = all
all: recurse
	mkdir -p target
	cp a.foo/target/a.foo.jar target/a.foo.jar
	../modularize-jar -u -f target/a.foo.jar --module-version 1.2.3 \
            -I module-info.java -p a.dep/target/a.dep/a.dep-1.0.3.jar
	../../diffcmd/diffcmd -d module-info-test -n module-info-test/normalizer ../../module-info/module-info target/a.foo.jar

recurse:
	$(MAKE) -C a.dep $(target)
	$(MAKE) -C a.foo $(target)

clean: target = clean
clean: recurse
	rm -rf target
	rm -rf module-info-test/actual
