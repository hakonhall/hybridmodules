all: main.jar

main.jar:
	modc -p ../m-1:../intermediate -m -o $@ -e main.Main -s src

clean:
	rm -rf main.jar target
