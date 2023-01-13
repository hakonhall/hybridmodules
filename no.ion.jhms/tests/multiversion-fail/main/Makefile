all: main.jar

main.jar:
	javac -d target/classes -p ../m-1:../intermediate `find src -name '*.java'`
	jar -c -f $@ -e main.Main -C target/classes .

clean:
	rm -rf main.jar target
