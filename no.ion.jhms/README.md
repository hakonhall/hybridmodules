# The javahms JHMS launcher

Contains the javahms program, which utilizes the no.ion.jhms Java library in
jhms to launch a Java Hybrid Module System application:

 1. Parses options and arguments, see 'javahms --help'.
 2. Launch no.ion.jhms.Main in class path mode.
 3. Main will start a hybrid module container, resolve the main hybrid module,
    and execute its main method.

## Tests

The tests directory contains conformance tests of javahms as a JHMS
implementation.
