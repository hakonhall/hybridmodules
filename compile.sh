#!/usr/bin/env bash

# Compiling java

function Fail {
    printf "%s" "$@"
    echo
    exit 1
}

function Compile {
    # Directory with java sources
    if test -d src
    then
	local srcdir=src
    elif test -d java
    then
	local srcdir=java
    else
	Fail "Found no source directories"
    fi

    local destdir=classes
    local version=1.0.0
    local jarpath=jar/out.jar
    local jardir="${jarpath%%/*}"
    local makefile=Makefile

    cat <<EOF > "$makefile"
javaPaths := \$(shell find $srcdir -name '*.java')
javaFiles := \$(patsubst $srcdir/%,%,\$(javaPaths))
classPaths := \$(patsubst $srcdir/%.java,classes/%.class,\$(javaPaths))
jarPath := $jarpath

all: \$(jarPath)

\$(jarPath): \$(classPaths)
	mkdir -p $jardir
	jar -c -f \$@ --module-version $version -C $destdir .

\$(classPaths): \$(javaPaths)
	javac -d $destdir \$^

clean:
	rm -rf $destdir $jardir

cleanall: clean
	@echo "Run compile.sh to recreate $makefile"
	rm -f $makefile
EOF

    make -f "$makefile"
}

function Main {
    Compile
}

Main "$@"
