# Multi-version failure test

This test is the same as the multiversion test, except that intermediate
`requires transitive m`, which has the effect that `main` both depends on m at
version 1 (m@1) directly and m@2 indirectly through intermediate.

Compiling `main` works fine*, however the failure is detected when building the
module graph at run time, which is verified by this test.

*) This is exactly the purpose of the javac wrapper of JHMS (§3.1), which is
 not yet implemented in this repository.
