# Java Hybrid Modules

See [Java Hybrid Modules Specification](
https://docs.google.com/document/d/1HJQi3nIEsFpn0IDIDXnNplRoFZEK7fNi4jFB50lvHtM/edit?usp=sharing). 
This README is implementation specific.

## Non Hybrid Modular Dependencies

In making a hybrid module, you may come across a dependency that is not yet provided as a hybrid module. 
Such a dependency may be a normal JAR intended to be put on the class path, an OSGi bundle JAR, or a modular JAR.

Ideally, the dependency should be made a hybrid module of course, and only that would have preserved the guarantees 
made by a fully hybrid module running application. This is often impractical or impossible and
we will describe a way to make the dependencies appear as hybrid modules. These dependencies will work
as proper hybrid modules, but only if there are no errors made in their conversion to the hybrid module, 
which is fragile since the java compiler cannot be used to help upholding the guarantees.

A modular JAR can almost always be taken verbatim as a hybrid modular JAR. But if you have several versions of that module,
then you must ensure each is updated with their version. The `make-modular-jar.sh` described below can be used for this.

There's a separate section below that deals with OSGi bundle JARs.

For a normal JAR you can use a tool `sh/make-modular-jar.sh` that compiles a `module-info.java`and updates the JAR 
to contain it, making it a modular JAR. Example:

```
sh/make-modular-jar.sh -u -f commons-lang-2.5.jar --module-info src --module-version 2.5
```

TODO: The above will probably not set the compiled version for the dependencies? Perhaps if they're included with -p?

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

* The version `@<version>` following the module name should be removed, but the version should be used in the following `make-modular-jar.sh` call.
* The dependant hybrid modules cannot be deduced by the OSGi metadata. The only time the tool gets this right is when there are no dependencies.
* If the OSGi bundle JAR contains embedded JAR dependencies, then those needs to be extracted into their own
  independent JAR files, and be made into hybrid modules, and must be referenced in `requires`.

Having made `module-info.java`, you can now call `make-modular-jar.sh`.
