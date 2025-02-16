# Overview
This is not a strict guideline on what "exactly" to do but rather a guide on what should likely be documented+how. 
Some pieces of documentation might need less/more than what is specified in this document depending on a number of factors.
Many functions/classes/fields may mot need documentation if their name/parameters/type can describe their use/behavior.

Helpful [resource](https://www.oracle.com/ca-en/technical-resources/articles/java/javadoc-tool.html) giving an overview of specific/appropriate usages for tags and docs.

# Java
### [code/server](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server)
- Documentation should always be in well formed javadoc on appropriate items (classes/methods/fields)
- javadoc should always use appropriate tags for appropriate reasons
- javadoc on methods should always include all parameters, return types, exceptions thrown, and type parameters
- javadoc on methods should describe the purpose of the method, its usage, and behavior **without unessesary** detail about its implementation
- javadoc on fields should only be included if **absolutely necessary** when the fields type+name does not provide sufficient information on its usage
- javadoc on classes should give a high level overview of what the class does and where it may be used

## Focus
### server
The majority of the focus will be on documenting [server](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/server)
since it is the most important to understand what the server does. 


[server/infrastructure/root](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/server/infrastructure/root)
Is where all the public API routes lie and is where most of the focus should lie.

### framework
[framework](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework) 
is where all the supporting code for the server lies and is largely resonsible for constructing, managing, and handling requests and routes.

Documentation for framework should not be a high priority and should be prioratized by the level of stability(more stable = better)
#### Stability
the code inside framework is much more subject to change so it has much less prioprity for complete documentation, especially the internal code which is not exposed to
[server](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/server)

#### Very Unstable
- [framework/web/route](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web/route)
- [framework/web/request](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web/request)
#### Unstable
- [framework/web](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web) the classes at the top level of this package may have methods changed/added/removed and may be split into smaller classes
- [framework/web/mail](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web) How mail is implemented may change
- [framework/web/annotations](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web) Annotations may be added/removed/renammed 
### Semi Stable
- [framework/web/error](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web/error) these are largely public facing for route errors and should probably be documented
- [framework/web/param/misc](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/web/param/misc) these classes usages will not change, but their implementation may
- [framework/util](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/util) is largely stable however, documentation isn't very needed.  
- [framework/db](https://github.com/DerfTastic/COSC_4P02/tree/main/code/server/app/src/main/java/framework/db) many of the classes are very unlikely to change, but may if their unstable counterparts change drastically


## Testing
Documentation in tests should describe what the test is doing, what result we are looking for, and what we hope to prove with the test.

# Javascript 
### [code/client](https://github.com/DerfTastic/COSC_4P02/tree/main/code/client)
Document defined methods. Include parameter and return types.
Define classes with expected fields and document types for those fields.
