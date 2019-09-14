all:
	javac -cp ../m1/m1.jar Main.java
	java -cp ../m1/m1.jar:../m2/m2.jar:. Main
	java -cp ../m2/m2.jar:../m1/m1.jar:. Main

clean:
	rm -f Main.class
