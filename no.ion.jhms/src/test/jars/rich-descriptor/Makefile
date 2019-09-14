all:
	rm -rf classes
	cp -r resources classes
	../../../../../modulec/modulec -d classes -p ../required -v 1.3.4 -e rich.descriptor.Main src

clean:
	rm -f rich.descriptor-1.3.4.jar
	rm -rf classes
