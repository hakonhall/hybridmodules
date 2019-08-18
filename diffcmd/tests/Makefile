all:
	cd basic && ../../diffcmd ../example-program one "t w o" 3
	cd basic && ../../diffcmd ../example-program one "t w o" 3
	cd basic && ../../diffcmd --clean
	cd basic && ../../diffcmd --clean
	mkdir -p empty
	cd empty && ../../diffcmd -d ../basic ../example-program one "t w o" 3
	cd empty && ../../diffcmd -d ../basic --clean
	# This uses diffcmd to verify the second diffcmd fails
	# correctly!
	cd cleanfails && ../../diffcmd bash -c 'cd baddir; ../../../diffcmd --clean'
	cd normalizer && ../../diffcmd -n ./normalizer.sh echo
	cd normalizer-fails && ../../diffcmd ../../diffcmd -n ./normalizer.sh echo

clean:
	cd basic && ../../diffcmd --clean
	cd cleanfails && ../../diffcmd --clean
	../diffcmd --clean
	../diffcmd --clean -d normalizer
	../diffcmd --clean -d normalizer-fails
