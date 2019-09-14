## Duplicate resource entries

This test is only using class path mode.  The Main class wants to load resource
"/META-INF/MANIFEST.MF", which is defined in both m1.jar and m2.jar.

Scenario 1: String.class.getResource(String) is used: No resource is found.

Scenario 2: C.class.getResource() is used, C being defined in m1: Resource is
found: The first MANIFEST.MF is found, searching in class path order.
