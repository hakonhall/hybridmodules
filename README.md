# Java Hybrid Modules System reference implementation

This repository contains the reference implementation of the Java Hybrid Module System, 3rd edition. The current specification is [here](
https://docs.google.com/document/d/1HJQi3nIEsFpn0IDIDXnNplRoFZEK7fNi4jFB50lvHtM/edit?usp=sharing). 

In short, Java Hybrid Module System (JHMS) is Java Platform Module System (JPMS) with multi-version support.

JHMS utilizes JPMS at compile and package time: modules are declared in module-info.java, the module path is used during compilation, and the module version can be set, etc.

However the dependencies seen at compile time, including the version of these (unlike JPMS), are also seen at run time. 
The only packages *visible* to a (hybrid) module at run time, are those that are are exported to the module from readable
modules. Accessibility in visible packages follows pre-JPMS accessibility, e.g. *setAccessible()* can be used to 
gain reflective access to non-public fields and method. The run time behavior is accomplished through class loader
techniques similar to OSGi.

The JHMS specification defines a set of relaxations to the strict specification that an implementation MAY implement at their discretion. The choices made in the reference implementation are detailed in the below Relaxation section.

In addition to the reference implementation, this repository also provides tools and tips for migrating plain JARs and OSGi bundles to JHMS in the below Migration section.

## Implementation notes

### Relaxations

Both Version relaxation (§4.1) and Automatic hybrid modules (§4.2) will be implemented by the reference implementation in a future version.

## Migration

In making a hybrid module, you may come across a dependency that is not yet provided as a hybrid module. 
Such a dependency may be a plain JAR intended to be put on the class path, an OSGi bundle JAR, or a modular JAR.

Ideally, all application JARs should be hybrid modular JARs. This is often impractical or impossible and
we will describe a way to make the dependencies appear as hybrid modules. These dependencies will work
as proper hybrid modules, but only if there are no errors made in their conversion to the hybrid module, 
which is fragile since the java compiler cannot be used to help upholding the guarantees.

A modular JAR can almost always be taken verbatim as a hybrid modular JAR. But if you have several versions of that module,
you must ensure each is updated with their version. The `modularize-jar` described below can be used for this.

There's a separate section below that deals with OSGi bundle JARs.

For a plain JAR you can use a tool `modularize-jar` that compiles a `module-info.java` and updates the JAR 
to contain it, making it a modular JAR. Example:

```
modularize-jar -u -f commons-lang-2.5.jar --module-info src --module-version 2.5
```

TODO: How to update the compiled version of the dependencies? Use --module-path with jar!? Can javac be used with --module-path (i.e. no source files - all .class files)?

### OSGi bundles

OSGi bundle JARs contains meta information that can be used to make a hybrid modular JAR:

* `Bundle-SymbolicName` may be suitable as the hybrid module name. Note that a valid module name must consist of valid Java identifiers separated by dots (.).
* `Bundle-Version` may be suitable as the hybrid module version.
* `Export-Package` lists the packages the JAR exports, and should match the `exports` in a `module-info.java`.
* `Import-Package` lists the packages the JAR depends on, and can be used to find which bundles it depends on, which may map 1:1 with dependent hybrid modules.

After building the module `bundle`, the tool `bundle/bundle.sh` can be used to extract this information in a way that looks deceivingly close to the content of a `module-info.java`.

```
bundle/bundle.sh BUNDLE > module-info.java
```

The resulting `module-info.java` needs to be fixed:

* The version `@<version>` following the module name should be removed, but the version should be used in the following `modularize-jar` call.
* The dependant hybrid modules cannot be deduced by the OSGi metadata. The only time the tool gets this right is when there are no dependencies.
* If the OSGi bundle JAR contains embedded JAR dependencies, then those needs to be extracted into their own
  independent JAR files, and be made into hybrid modules, and must be referenced in `requires`.

Having made `module-info.java`, you can now call `modularize-jar`.
