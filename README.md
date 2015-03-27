Paint-Upload
=========

[![Apache 2 License](https://img.shields.io/badge/license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.org/Elopteryx/paint-upload.svg?branch=master)](https://travis-ci.org/Elopteryx/paint-upload)
[![Coverage Status](https://coveralls.io/repos/Elopteryx/paint-upload/badge.svg)](https://coveralls.io/r/Elopteryx/paint-upload)

Paint-Upload is a file upload library for servlets and web applications. Although you can already use the standard
servlet API to retrieve part items from a multipart request this library provides extra functionality not found
elsewhere. First, it has a fluent API which allows you to completely control the uploading process. No more waiting for
the user to upload everything, you can run your business logic, like file type validation and writing to a permanent
location as soon as the bytes arrive. Another great feature is that if you choose it and your servlet supports it the
upload request can run in asynchronous mode, using the async IO API introduced in the 3.1 version of the servlet API.
This will allow a much better use of system resources, no more waiting threads.

The library has two components. The core module is written for plain servlets, although it can work with any web
framework that allows access to the HttpServletRequest instance, for example Spring WebMVC. The jaxrs module is still
work in progress, I'm still experimenting with it. In the future it should allow you to write compact and reusable code
for your endpoints.

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
* Java 7+
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

Does the library have bugs? Needs extra functionality? Do you like the API? Feel free to create an issue!

Examples
--------

Very simple code when running on Java 8, have a servlet which is set to support async mode, but has no MultiPartConfig,
import the UploadParser class and pass your logic. The parser class will take care of starting async mode and
registering the necessary classes to start the parsing. Note that even if you can't enable async support
for your servlet the parser will still work, but it will use blocking mode. Your functions will still be called
just like in async mode. Everything works the same on Java 7, the difference is that
you'll have to use anonymous classes and implement the functional interfaces of this library.

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
                    .onRequestComplete(context -> context.getUserObject(HttpServletResponse.class).setStatus(200))
                    .onError((context, throwable) -> {
                        throwable.printStackTrace();
                        response.sendError(500);
                    })
                    .sizeThreshold(4096)
                    .maxPartSize(1024 * 1024 * 25)
                    .maxRequestSize(1024 * 1024 * 500)
                    .setupAsyncParse(request);
        }
}
```

You can also use the parser with web frameworks. The following example shows how to use it with a Jax-RS endpoint:

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
            //Your business logic here, check the part, you can use the bytes in the buffer to check
            //the real mime type, then return with a channel, stream or path to write the part
            return PartOutput.from(new NullChannel());
        }

        @Override
        public void onPartEnd(UploadContext context) throws IOException {
            //Your business logic here
        }

        @Override
        public void onRequestComplete(UploadContext context) throws IOException, ServletException {
            //Your business logic here, send a response to the client
            context.getUserObject(AsyncResponse.class).resume(Response.ok().build());
        }

        @Override
        public void onError(UploadContext context, Throwable throwable) {
            //Your business logic here, handle the error
            context.getUserObject(AsyncResponse.class).resume(Response.serverError().build());
        }
    }
```

Gradle
-----
```xml
compile "com.github.elopteryx:paint-upload:1.2.0"
```
Maven
-----
```xml
<dependency>
    <groupId>com.github.elopteryx</groupId>
    <artifactId>paint-upload</artifactId>
    <version>1.2.0</version>
</dependency>
```

Find available versions on [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.elopteryx%22%20AND%20a%3A%22paint-upload%22).