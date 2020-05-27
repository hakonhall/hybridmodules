all: main.jar

main.jar:
	modulec -f $@ -p ../m-1:../intermediate -e main.Main src

clean:
	rm -f main.jar
