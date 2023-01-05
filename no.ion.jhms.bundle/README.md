# Library and program to read OSGi bundle JARs

## TODO

1. Read a complete set of bundles, that only references each other, to deduce
the meaning of the `Import-Package`, and thereby filling in `requires`.

1. Support reading `uses` directives in the `Export-Package` manifest entries, 
and transform these to `requires transitive` in the `module-info.java`.
