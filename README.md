  Copyright 2015- Adam Forgacs
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

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