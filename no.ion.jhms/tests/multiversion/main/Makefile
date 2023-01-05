all: main.jar

main.jar:
	modc -p ../m-1:../intermediate/target/intermediate -m -e main.Main -s src

clean:
	rm -rf main.jar target
