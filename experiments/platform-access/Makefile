all: compile test

compile:
	modulec -f a.jar src

test:
	@ # Verify that without --add-opens we get a warning for reflective
	@ # access to a field in the java.logging module.
	@ # The redirects doesn't seem to be handled correctly by Make
	bash -c 'java -cp a.jar a.A &> out'
	grep -q 'Illegal reflective access by a.A' out
	grep -q 'to field java.util.logging.Logger.offValue' out

	@ # Verify that with --add-opens there is no warning
	bash -c 'java --add-opens java.logging/java.util.logging=ALL-UNNAMED -cp a.jar a.A 2> out > /dev/null'
	test -e out && ! test -s out

clean:
	rm -f out a.jar
	rm -rf target
