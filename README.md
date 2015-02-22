Paint-Upload
=========

[![Build Status](https://travis-ci.org/Elopteryx/paint-upload.svg?branch=master)](https://travis-ci.org/Elopteryx/paint-upload)

Paint-Upload is an asynchronous file upload library, allowing non-blocking parsing of
multipart requests, using the async IO API introduced with Servlet 3.1.

The actual multipart parser is based on the parser from Undertow. Using
that I created a custom ReadListener implementation, which can be customized with a 
fluent API, however you wish.

Features
--------
* Async parsing, preventing IO blocking
* Fully customizable, just pass your custom logic
  * ```.onPartBegin(…)```
  * ```.onPartEnd(…)```
  * ```.onRequestComplete(…)```
  * ```.onError(…)```
* Lightweight, no dependencies other than the servlet API
* Available from the Maven Central repository

Requirements
--------
* Java 7+
* Servlet 3.1 environment

Gradle
-----
```xml
compile "com.github.elopteryx:paint-upload:1.1.0"
```
Maven
-----
```xml
<dependency>
    <groupId>com.github.elopteryx</groupId>
    <artifactId>paint-upload</artifactId>
    <version>1.1.0</version>
</dependency>
```

Find available versions on [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.elopteryx%22%20AND%20a%3A%22paint-upload%22).