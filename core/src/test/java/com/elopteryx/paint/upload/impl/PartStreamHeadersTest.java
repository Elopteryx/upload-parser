package com.elopteryx.paint.upload.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Iterator;

public class PartStreamHeadersTest {

    @Test
    public void add_and_retrieve_headers() throws Exception {
        PartStreamHeaders partStreamHeaders = new PartStreamHeaders();
        partStreamHeaders.addHeader("Content-Disposition", "form-data; name=\"FileItem\"; filename=\"file1.txt\"");
        partStreamHeaders.addHeader("Content-Type", "text/plain");
        partStreamHeaders.addHeader("TestHeader", "headerValue1");
        partStreamHeaders.addHeader("TestHeader", "headerValue2");
        partStreamHeaders.addHeader("TestHeader", "headerValue3");
        partStreamHeaders.addHeader("testheader", "headerValue4");

        Iterator<String> headerNameEnumeration = partStreamHeaders.getHeaderNames().iterator();
        assertEquals("content-disposition", headerNameEnumeration.next());
        assertEquals("content-type", headerNameEnumeration.next());
        assertEquals("testheader", headerNameEnumeration.next());
        assertFalse(headerNameEnumeration.hasNext());

        assertEquals(partStreamHeaders.getHeader("Content-Disposition"), "form-data; name=\"FileItem\"; filename=\"file1.txt\"");
        assertEquals(partStreamHeaders.getHeader("Content-Type"), "text/plain");
        assertEquals(partStreamHeaders.getHeader("content-type"), "text/plain");
        assertEquals(partStreamHeaders.getHeader("TestHeader"), "headerValue1");
        assertNull(partStreamHeaders.getHeader("DummyHeader"));

        Iterator<String> headerValueEnumeration;

        headerValueEnumeration = partStreamHeaders.getHeaders("Content-Type").iterator();
        assertTrue(headerValueEnumeration.hasNext());
        assertEquals(headerValueEnumeration.next(), "text/plain");
        assertFalse(headerValueEnumeration.hasNext());

        headerValueEnumeration = partStreamHeaders.getHeaders("content-type").iterator();
        assertTrue(headerValueEnumeration.hasNext());
        assertEquals(headerValueEnumeration.next(), "text/plain");
        assertFalse(headerValueEnumeration.hasNext());

        headerValueEnumeration = partStreamHeaders.getHeaders("TestHeader").iterator();
        assertTrue(headerValueEnumeration.hasNext());
        assertEquals(headerValueEnumeration.next(), "headerValue1");
        assertTrue(headerValueEnumeration.hasNext());
        assertEquals(headerValueEnumeration.next(), "headerValue2");
        assertTrue(headerValueEnumeration.hasNext());
        assertEquals(headerValueEnumeration.next(), "headerValue3");
        assertTrue(headerValueEnumeration.hasNext());
        assertEquals(headerValueEnumeration.next(), "headerValue4");
        assertFalse(headerValueEnumeration.hasNext());

        headerValueEnumeration = partStreamHeaders.getHeaders("DummyHeader").iterator();
        assertFalse(headerValueEnumeration.hasNext());
    }

    @Test
    public void charset_parsing() {
        assertEquals(null, PartStreamHeaders.extractQuotedValueFromHeader("text/html; other-data=\"charset=UTF-8\"", "charset"));
        assertEquals(null, PartStreamHeaders.extractQuotedValueFromHeader("text/html;", "charset"));
        assertEquals("UTF-8", PartStreamHeaders.extractQuotedValueFromHeader("text/html; charset=\"UTF-8\"", "charset"));
        assertEquals("UTF-8", PartStreamHeaders.extractQuotedValueFromHeader("text/html; charset=UTF-8", "charset"));
        assertEquals("UTF-8", PartStreamHeaders.extractQuotedValueFromHeader("text/html; charset=\"UTF-8\"; foo=bar", "charset"));
        assertEquals("UTF-8", PartStreamHeaders.extractQuotedValueFromHeader("text/html; charset=UTF-8 foo=bar", "charset"));
    }

    @Test
    public void extract_existing_boundary() {
        assertEquals("--xyz", PartStreamHeaders.extractBoundaryFromHeader("multipart/form-data; boundary=--xyz; param=abc"));
    }

    @Test
    public void extract_missing_boundary() {
        assertNull(PartStreamHeaders.extractBoundaryFromHeader("multipart/form-data; boundary;"));
    }

    @Test
    public void extract_param_with_tailing_whitespace() {
        assertEquals("abc", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=abc", "param"));
        assertEquals("abc", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=abc ", "param"));
        assertEquals("", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param= abc", "param"));
        assertEquals("", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param= abc ", "param"));

        assertEquals("abc", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\"abc\"", "param"));
        assertEquals("abc ", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\"abc \"", "param"));
        assertEquals(" abc", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\" abc\"", "param"));
        assertEquals(" abc ", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\" abc \"", "param"));

        assertNull(PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param\t=abc", "param"));
        assertEquals("ab", PartStreamHeaders.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=ab\tc", "param"));
    }
}
