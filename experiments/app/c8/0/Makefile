all: root.jar

root.jar:
	modulec -f $@ -e root.Main -p ../A:../B java

clean:
	rm -f root.jar
	rm -rf target
