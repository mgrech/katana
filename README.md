# The Katana programming language

## Build instructions
**Java 10** and **Maven 3.x** are required. After running `mvn package`, a self-contained zip archive is created in the release/target/ subdirectory containing the Katana compiler as well as a minimal JRE to run it.

## Runtime dependencies
Katana requires a relatively recent version of **clang** on the system PATH that matches the bitness of the JVM Katana was built with.
On Windows, **Visual Studio or the Visual Studio Build Tools** are needed to get the C/C++ standard libraries.

## Installation
Katana does not need to be installed and can run from its unzipped directory, however it is usually convenient to add it to the system PATH.