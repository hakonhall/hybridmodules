# Java Hybrid Modules

See [Java Hybrid Modules Specification](
https://docs.google.com/document/d/1HJQi3nIEsFpn0IDIDXnNplRoFZEK7fNi4jFB50lvHtM/edit?usp=sharing). 
This README is implementation specific.

## Non Hybrid Modular JARs

hybridmodules provides a tool `sh/make-modular-jar.sh` that takes a non-modular JAR and a `module-info.java`,
compiles the latter and updates the JAR to contain it, making it a modular JAR. Example:

```
sh/make-modular-jar.sh -u -f commons-lang-2.5.jar --module-info src/module-info.java --module-version 2.5
```
