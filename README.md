# librsyncWrapper
A thin Java JNI wrapper around the C librsync library, making it easy to use the librsync library from Java code.

The librsync library (which this repository wraps) can be found here:  https://github.com/librsync/librsync

# Example
The LibrsyncWrapperTest class is an example of how to use the LibrsyncWrapper to execute librsync operations
from Java.  It is a simple command line program that demonstrates the functionality provided by this repository.
See the LibrsyncWrapperTest class for more information.

# Using the LibrsyncWrapper
## Prerequisites
This repository is built on top of the librsync library.  In order to build the code provided in this repository
and/or run the example LibrsyncWrapperTest program, you must already have access to the librsync library.
The librsync library upon which this repository is built can be found here:  https://github.com/librsync/librsync

The specific librsync files that are required by the LibrsyncWrapper are:
* librsync.h  - Can be found in the librsync repository
* librsync-config.h - Can be found in the librsync repository
* librsync.so - Output of the librsync build
* librsync.so.1 - Output of the librsync build

## Building
This repository contains a very rudimentary example script for Linux (really just a list of commands) that can be
executed to build the LibrsyncWrapper library and run the LibrsyncWrapperTest.  However, before running the script the
first time, there are some manual steps that must be performed to copy the required librsync files to this repository's
directory.  Please perform these steps:

* Copy librsync.h and librsync-config.h (both part of the librsync repository) to the c directory
* Copy librsync.so and librsync.so.1 (output by the librsync build) to the lib directory

Once you have done the above, you can run the following from the top level librsyncWrapper directory:
 make

