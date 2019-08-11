all:
	../command-test -C basic ../example-program one "t w o" 3
	../command-test -C basic ../example-program one "t w o" 3
	../command-test -C basic --clean
	../command-test --clean -C basic
	cd basic && ../../command-test ../example-program one "t w o" 3
	cd basic && ../../command-test --clean
	# This uses command-test to verify the second command-test fails
	# correctly
	../command-test -C cleanfails ../../command-test -C baddir --clean

clean:
	../command-test --clean
