.PHONY: all force-jar install clean

all: force-jar target/bundle

force-jar target/bundle-1.0-SNAPSHOT.jar:
	mvn -nsu package

target/bundle: bin/bundle.sh target/bundle-1.0-SNAPSHOT.jar
	cat $^ > $@
	chmod +x $@


install: ~/bin/bundle

~/bin/bundle: target/bundle
	cp $^ $@


clean:
	mvn -nsu clean


cleanall: clean
	rm -f ~/bin/bundle
