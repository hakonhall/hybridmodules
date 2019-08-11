#!/bin/bash

function Usage {
    cat <<EOF
Usage: ${0##*/} -d DEST JAR
Extract info from OSGi JAR to help make a module-info.java.

Prints information from the OSGi bundle that makes it easy to create a
module-info.java, of the format:

    open module <Bundle-SymbolicName>@<Bundle-Version> {
      exports <Export-Package1>;
      ...
      exports <Export-PackageN>;
    }

The @<Bundle-Version> should be stripped and used with jar --module-version.
EOF

    exit 0
}

function Fail {
    printf "%s" "$@"
    printf "\n"
    exit 1
}

function Main {
    local dir="${0%/*}"
    local jar="$dir"/target/bundle-1.0-SNAPSHOT.jar
    if ! test -r "$jar"
    then
        Fail "Failed to find jar at '$jar'"
    fi

    java -cp "$jar" no.ion.jhms.bundle.Main "$@"
}

Main "$@"
