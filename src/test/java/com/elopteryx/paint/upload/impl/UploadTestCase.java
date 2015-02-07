package com.elopteryx.paint.upload.impl;

class UploadTestCase {

    private final String fileName = "foo.png";
    final String request =
            "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                    "Content-Type: text/whatever\r\n" +
                    "\r\n" +
                    "This is the content of the file\n" +
                    "\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"field\"\r\n" +
                    "\r\n" +
                    "fieldValue\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value1\r\n" +
                    "-----1234\r\n" +
                    "Content-Disposition: form-data; name=\"multi\"\r\n" +
                    "\r\n" +
                    "value2\r\n" +
                    "-----1234--\r\n";

}
