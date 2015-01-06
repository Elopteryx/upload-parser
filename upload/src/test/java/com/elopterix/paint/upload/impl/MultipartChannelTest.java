package com.elopterix.paint.upload.impl;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class MultipartChannelTest {

    static private final String BOUNDARY_TEXT = "myboundary";

    @Test
    public void testThreeParamConstructor() throws Exception {
        final String strData = "foobar";
        final byte[] contents = strData.getBytes();
        InputStream input = new ByteArrayInputStream(contents);
        byte[] boundary = BOUNDARY_TEXT.getBytes();
        MultipartChannel ms = new MultipartChannel(input, boundary);
        assertNotNull(ms);
    }
}
