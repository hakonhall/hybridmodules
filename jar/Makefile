all:
	$(MAKE) -C test

install: ~/bin/modularize-jar

~/bin/modularize-jar: modularize-jar
	cp $< $@

clean:
	$(MAKE) -C test clean
