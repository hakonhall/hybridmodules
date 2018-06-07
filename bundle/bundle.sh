#!/bin/bash

function Usage {
    cat <<EOF
Usage: ${0##*/} JAR
Extract info from OSGi JAR to help make a module-info.java.

Prints information from the OSGi bundle that makes it easy to create a
module-info.java, of the format:

    module <Bundle-SymbolicName>@<Bundle-Version> {
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
    while (( $# > 0 ))
    do
        case "$1" in
            -h|--help|help) Usage ;;
            *) break ;;
        esac
    done

    if (( $# == 0 ))
    then
        Usage
    fi

    local dir="${0%/*}"
    local jar="$dir"/target/bundle-1.0-SNAPSHOT.jar
    if ! test -r "$jar"
    then
        Fail "Failed to find jar at '$jar'"
    fi

    if (( ${#JAVA_HOME} > 0 ))
    then
        local java="$JAVA_HOME"/bin/java
    else
        local java=java
    fi

    "$java" -cp "$jar" no.ion.hybridmodules.bundle.Main "$@"
}

Main "$@"
