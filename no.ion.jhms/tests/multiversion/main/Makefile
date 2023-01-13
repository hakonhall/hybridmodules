all: main.jar

main.jar:
	javac -d target/classes -p ../m-1:../intermediate/target `find src -name '*.java'`
	jar -c -f target/$@ -e main.Main -C target/classes .

clean:
	rm -rf main.jar target
