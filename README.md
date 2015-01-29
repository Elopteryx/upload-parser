Paint-Upload
=========

Paint-Upload is an asynchronous file upload library, allowing non-blocking parsing of
multipart requests, using the async IO API introduced with Servlet 3.1.

The actual multipart parser is based on the parser from Undertow. Using
that I created a custom ReadListener implementation, which can be customized with a 
fluent API, however you wish.

Features
--------
* Async parsing, preventing IO blocking
* Fully customizable, just pass your custom logic
  * ```.onPartStart(…)```
  * ```.onPartFinish(…)```
  * ```.onComplete(…)```
  * ```.onError(…)```
* Lightweight, no dependencies other than the servlet API

Requirements
--------
* Java 7+
* Servlet 3.1 environment