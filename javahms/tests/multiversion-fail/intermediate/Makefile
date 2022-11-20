all: intermediate.jar

intermediate.jar:
	modc -p ../m-2 -m -o $@ -s src

clean:
	rm -f intermediate.jar
	rm -rf target
