#!/usr/bin/env bash

function Usage {
    cat <<EOF
Usage: javahms [OPTION...] --module MODULE[/[/]CLASS] [ARG...]
Launch hybrid module MODULE, passing ARG... to the main method of CLASS.

Launch Java Hybrid Module System (JHMS) application.

CLASS must be an exported class of MODULE.  ARG... are passed to the main
method of CLASS.

ARG... are passed to the main method of CLASS of module MODULE. CLASS must be
exported by MODULE.

Options:
  --context-class-loader,-c MODULE
      Set the current thread's context class loader to the one associated with
      the hybrid module with the given name and version, instead of that
      associated with --module.
  --java-options,-j TOK JAVA_OPTION... TOK
      All command-line arguments following TOK up to but not including the next
      TOK (JAVA_OPTION...) will be passed through to the java invocation.
      JAVA_OPTIONS... must not specify the class path. As a special case: if
      the first TOK is one of (, {, or [, the end token must be ), }, or ],
      respectively. See below for more.
  --module,-m MODULE[/[/]CLASS]
      Specifies the main module and class used to launch the application.
      CLASS defaults to the main class of MODULE.  MODULE//CLASS means the
      qualified main class is MODULE.CLASS.  This option is required and is the
      last option. The next argument (if any) is the first ARG.
  --module-graph,-g GRAPH_OPTIONS
      Probe the module graph instead of running the main method. See MODULE
      GRAPH below for more details.
  --module-path,-p PATH
      A : separated list of paths, each path is a path to a hybrid modular JAR
      file or a directory containing such files.

To pass java command-line arguments (JAVA_OPTIONS...) to the java invocation
when launching a JHMS application, the administrator would pick a token (TOK)
distinct from all command-line arguments (JAVA_OPTIONS), and then use the
--java-options option.  As special cases, "{" is ended by "}", and similarly
for parentheses and brackets. Examples passing '-Xms1G' and '-Xmx2G' to java:

  javahms -j { -Xms1G -Xmx2G } -p mods -m super.module arg1 arg2
  javahms -j JVMLIST -Xms1G -Xmx2G JVMLIST -p mods -m super.module arg1 arg2

MODULE GRAPH

The --module-graph takes one argument, GRAPH_OPTIONS, which is a
comma-separated list of:
  exports        Include packages that are 1. exported unqualified on each
                 module, 2. qualified exported on read edges, and 3. (with
                 'self'), implicit qualified exported to itself, i.e. all those
                 packages that have not been exported.
  noplatform     Do not include platform modules.
  self           Also include the read edges from each module to themselves.
  visible        Only include modules readable by the root modules.
  -MODULE        Exclude MODULE from the graph (either NAME@VERSION for hybrid
                 modules or NAME for platform modules).

The module graph is printed to stdout. Pipe this to a file (foo.dot). The .dot
file can be viewed by e.g. invoking
  dot -Tsvg foo.dot > foo.html
and viewing the HTML file in a browser.
EOF

    exit 0
}

function Fail {
    printf "%s" "$@"
    echo
    exit 1
}

function Main {
    (( $# > 0 )) || Fail "Missing '--module', try --help/-h?"

    local -a java_options=()
    local -a jhms_args=()
    local -a module=()

    while (( $# > 0 ))
    do
        case "$1" in
            -c|--context-class-loader)
                jhms_args+=("$1" "$2")
                shift 2 || true
                ;;
            --help|-h) Usage ;;
            --java-options|-j)
                shift
                case "$1" in
                    '(') local end_token=')' ;;
                    '{') local end_token='}' ;;
                    '[') local end_token=']' ;;
                    *) local end_token="$1" ;;
                esac
                shift || true

                while true
                do
                    if (( $# == 0 ))
                    then
                        Fail "Failed to find end token of the java options " \
                             "($end_token)"
                    fi

                    if test "$1" == "$end_token"
                    then
                        shift
                        break
                    else
                        java_options+=("$1")
                        shift
                    fi
                done
                ;;
            --module-graph|-g)
                jhms_args+=("$1" "$2")
                shift 2 || true
                ;;
            --module-path|-p)
                jhms_args+=("$1" "$2")
                shift 2 || true
                ;;
            --module|-m)
                if [[ "$2" =~ ^([^/]*)//(.*)$ ]]
                then
                    module=("$1" "${BASH_REMATCH[1]}"/"${BASH_REMATCH[1]}.${BASH_REMATCH[2]}")
                else
                    module=("$1" "$2")
                fi
                shift 2 || true
                break
                ;;
            *) Fail "Unknown option '$1'" ;;
        esac
    done

    exec java "${java_options[@]}" -jar "$0" "${jhms_args[@]}"  "${module[@]}" "$@"
}

Main "$@"
