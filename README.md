Upload Parser
=========

[![Apache 2 License](https://img.shields.io/badge/license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Actions Status](https://github.com/Elopteryx/upload-parser/workflows/Upload%20Parser%20CI/badge.svg)](https://github.com/Elopteryx/upload-parser/actions)
[![Coverage Status](https://coveralls.io/repos/github/Elopteryx/upload-parser/badge.svg?branch=master)](https://coveralls.io/github/Elopteryx/upload-parser?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.elopteryx/upload-parser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.elopteryx/upload-parser)
[![JavaDoc](https://img.shields.io/badge/javadoc-3.0.0-brightgreen.svg)](http://www.javadoc.io/doc/com.github.elopteryx/upload-parser)

Upload Parser is a file upload library for servlets and web applications. Although you can already use the standard
servlet API to retrieve part items from a multipart request this library provides extra functionality not found
elsewhere. First, it has a fluent API which allows you to completely control the uploading process. No more waiting for
the user to upload everything, you can run your business logic, like file type validation and writing to a permanent
location as soon as the bytes arrive. Another great feature is that if you choose it and your servlet supports it the
upload request can run in asynchronous mode, using the async IO API introduced in the 3.1 version of the servlet API.
This will allow a much better use of system resources, no more waiting threads.

The library has two components. The core module is written for plain servlets, although it can work with any web
framework that allows access to the HttpServletRequest instance, for example Spring WebMVC. The JAX-RS module allows
you to handle the uploaded parts as automatically injected method parameters when using the JAX-RS runtime.

I consider the modules complete, when it comes to features. I don't think I can add more without bloating the
library, but if you have suggestions for new stuff, or if you have found a bug I would be more than happy to fix that.
If a new version of Java SE or EE comes out I will try to experiment with it, maybe make a new major version.

Features
--------
* Async and blocking multipart request parsing
* Unopinionated, fully customizable, just pass your custom logic
  * ```.onPartBegin(…)``` when the client starts sending a part, with optional buffering
  * ```.onPartEnd(…)``` when the client finishes sending a part
  * ```.onRequestComplete(…)``` after everything has been uploaded
  * ```.onError(…)``` if an error occurs
* Lightweight, less than 40Kb size, no dependencies other than the servlet API
* Available from the Maven Central repository

Requirements
--------
* Java 11 for version 3.0.0, Java 8 for version 2.2.1
* Servlet 3.1 environment

Motivation
--------

Although the Servlet API already has support for handling multipart requests since version 3.0, I found it lacking in several situations.
First, it uses blocking IO which can cause performance problems, especially because an upload can take a very long time. To fix this problem in general,
the ReadListener and WriteListener interfaces have been introduced in version 3.1, to prevent blocking, but to use them, you have to use your own parsing
code, you can't rely on the servlet container to do the job for you in the async mode.

Also, the classic blocking method parses the whole request
and only lets you run your code after it is finished. Why no support to check for the correct file extension, or request size right when they become available?
And so I decided to write this small library which handles those situation. If you don't have these requirements then the Servlet API will do the job
just fine. Otherwise, I think you will find my library useful.

Issues
------

Does the library have bugs? Needs extra functionality? Do you like the API? Feel free to open an issue!

Examples
--------

Very simple to setup, have a servlet which has no MultiPartConfig annotation or the identical section in the web.xml,
import the UploadParser class and pass your logic. The parser class will take care of starting async mode and
registering the necessary classes to start the parsing. Note that if you can't enable async support
for your servlet then you can also use blocking mode, by calling the doBlockingParse method instead. Your functions will
still be called just like in async mode.

```java

    @WebServlet(value = "/UploadServlet", asyncSupported = true)
    public class UploadServlet extends HttpServlet {

        /**
         * Directory where uploaded files will be saved, relative to
         * the web application directory.
         */
        private static final String UPLOAD_DIR = "uploads";

        protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

            String applicationPath = request.getServletContext().getRealPath("");
            final Path uploadFilePath = Paths.get(applicationPath, UPLOAD_DIR);

            if (!Files.isDirectory(uploadFilePath)) {
                Files.createDirectories(uploadFilePath);
            }

            // Check that we have a file upload request
            if (!UploadParser.isMultipart(request)) {
                return;
            }

            UploadParser.newParser()
                    .onPartBegin((context, buffer) -> {
                        PartStream part = context.getCurrentPart();
                        Path path = uploadFilePath.resolve(part.getSubmittedFileName());
                        return PartOutput.from(path);
                    })
                    .onRequestComplete(context -> response.setStatus(200))
                    .onError((context, throwable) -> {
                        response.sendError(500);
                    })
                    .sizeThreshold(4096)
                    .maxPartSize(1024 * 1024 * 25)
                    .maxRequestSize(1024 * 1024 * 500)
                    .setupAsyncParse(request);
        }
}
```

You can also use the parser with web frameworks. The following example shows how to use it with a JAX-RS endpoint:

```java

    @Path("upload")
    public class UploadController implements OnPartBegin, OnPartEnd, OnRequestComplete, OnError {

        @POST
        @Path("upload")
        public void multipart(@Context HttpServletRequest request, @Suspended final AsyncResponse asyncResponse) throws IOException, ServletException {
            UploadParser.newParser()
                    .onPartBegin(this)
                    .onPartEnd(this)
                    .onRequestComplete(this)
                    .onError(this)
                    .userObject(asyncResponse)
                    .setupAsyncParse(request);
        }

        @Override
        @Nonnull
        public PartOutput onPartBegin(UploadContext context, ByteBuffer buffer) throws IOException {
            // Your business logic here, check the part, you can use the bytes in the buffer to check
            // the real mime type, then return with a channel, stream or path to write the part
            return PartOutput.from(new NullChannel());
        }

        @Override
        public void onPartEnd(UploadContext context) throws IOException {
            // Your business logic here
        }

        @Override
        public void onRequestComplete(UploadContext context) throws IOException, ServletException {
            // Your business logic here, send a response to the client
            context.getUserObject(AsyncResponse.class).resume(Response.ok().build());
        }

        @Override
        public void onError(UploadContext context, Throwable throwable) {
            // Your business logic here, handle the error
            context.getUserObject(AsyncResponse.class).resume(Response.serverError().build());
        }
    }
```

These two examples show how to use the core library. If you want to handle multipart requests in a more elegant way, you
can use the JAX-RS module. It comes with a message body reader implementation (although it has to be extended and
registered manually) and configuration annotations which allows you to receive the request parts as method parameters.
The following code shows how they work:

```java

    @Path("upload")
    public class ReaderUploadController {
    
        /**
         * Example endpoint for the reader, the reader injects the object representing the whole
         * multipart request.
         * @param multiPart The multipart request
         * @throws IOException If an error occurred with the I/O
         * @throws ServletException If an error occurred with the servlet
         */
        @POST
        @Path("upload1")
        public void multiPart(@UploadConfig(sizeThreshold = 4096)MultiPart multiPart) throws IOException, ServletException {
            multiPart.getParts();
            multiPart.getHeaders();
            multiPart.getSize();
            // ...
        }
    
        /**
         * Example endpoint for the reader, the reader injects the part objects as method parameters.
         * @param part1 The first part, with the given form name
         * @param part2 The second part, with the given form name
         * @param part3 The third part, with the given form name
         * @throws IOException If an error occurred with the I/O
         * @throws ServletException If an error occurred with the servlet
         */
        @POST
        @Path("upload2")
        public void separateParts(@UploadParam("text1")Part part1,
                                  @UploadParam("text2")Part part2,
                                  @UploadParam("file")Part part3
        ) throws IOException, ServletException {
            // ...
        }
    }

```

Note that the JAX-RS API does not support async IO for message body readers, therefore the parsing can only work in a
blocking mode, if you use it like in the last example. If you are not planning to use parameter injection, then 
importing the JAX-RS module is unnecessary, the core library will also work, as shown in the second example.

For more information, please check the javadoc:

Core ([javadoc][1])

JAX-RS ([javadoc][2])

Gradle
-----

**For use with Java 11+**

```xml
compile 'com.github.elopteryx:upload-parser:3.0.0'

compile 'com.github.elopteryx:upload-parser-jaxrs:3.0.0'
```

**For use with Java 8+**

```xml
compile 'com.github.elopteryx:upload-parser:2.2.1'

compile 'com.github.elopteryx:upload-parser-jaxrs:2.2.1'

```
Maven
-----

**For use with Java 11+**

```xml
<dependency>
    <groupId>com.github.elopteryx</groupId>
    <artifactId>upload-parser</artifactId>
    <version>3.0.0</version>
</dependency>

<dependency>
    <groupId>com.github.elopteryx</groupId>
    <artifactId>upload-parser-jaxrs</artifactId>
    <version>3.0.0</version>
</dependency>
```

**For use with Java 8+**

```xml
<dependency>
    <groupId>com.github.elopteryx</groupId>
    <artifactId>upload-parser</artifactId>
    <version>2.2.1</version>
</dependency>

<dependency>
    <groupId>com.github.elopteryx</groupId>
    <artifactId>upload-parser-jaxrs</artifactId>
    <version>2.2.1</version>
</dependency>
```

Find available versions on [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.elopteryx%22%20AND%20a%3A%22upload-parser%22).

[1]: http://www.javadoc.io/doc/com.github.elopteryx/upload-parser/3.0.0
[2]: http://www.javadoc.io/doc/com.github.elopteryx/upload-parser-jaxrs/3.0.0
