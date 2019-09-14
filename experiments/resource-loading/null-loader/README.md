## Test what classes have a null class loader

String.class.getClassLoader() is null. Same with List. So all platform classes
have null class loader. This proves Class.getClassLoader0() returns null for
platform classes.
