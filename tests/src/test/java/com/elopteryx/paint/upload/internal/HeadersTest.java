package com.elopteryx.paint.upload.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Iterator;

public class HeadersTest {

    @Test
    public void add_and_retrieve_headers() throws Exception {
        Headers headers = new Headers();
        headers.addHeader(Headers.CONTENT_DISPOSITION, "form-data; name=\"FileItem\"; filename=\"file1.txt\"");
        headers.addHeader(Headers.CONTENT_TYPE, "text/plain");
        headers.addHeader("TestHeader", "headerValue1");
        headers.addHeader("TestHeader", "headerValue2");
        headers.addHeader("TestHeader", "headerValue3");
        headers.addHeader("testheader", "headerValue4");

        Iterator<String> headerNames = headers.getHeaderNames().iterator();
        assertEquals(Headers.CONTENT_DISPOSITION.toLowerCase(), headerNames.next());
        assertEquals(Headers.CONTENT_TYPE.toLowerCase(), headerNames.next());
        assertEquals("testheader", headerNames.next());
        assertFalse(headerNames.hasNext());

        assertEquals(headers.getHeader(Headers.CONTENT_DISPOSITION), "form-data; name=\"FileItem\"; filename=\"file1.txt\"");
        assertEquals(headers.getHeader(Headers.CONTENT_TYPE), "text/plain");
        assertEquals(headers.getHeader(Headers.CONTENT_TYPE), "text/plain");
        assertEquals(headers.getHeader("TestHeader"), "headerValue1");
        assertNull(headers.getHeader("DummyHeader"));

        Iterator<String> headerValues;

        headerValues = headers.getHeaders("Content-Type").iterator();
        assertTrue(headerValues.hasNext());
        assertEquals(headerValues.next(), "text/plain");
        assertFalse(headerValues.hasNext());

        headerValues = headers.getHeaders("content-type").iterator();
        assertTrue(headerValues.hasNext());
        assertEquals(headerValues.next(), "text/plain");
        assertFalse(headerValues.hasNext());

        headerValues = headers.getHeaders("TestHeader").iterator();
        assertTrue(headerValues.hasNext());
        assertEquals(headerValues.next(), "headerValue1");
        assertTrue(headerValues.hasNext());
        assertEquals(headerValues.next(), "headerValue2");
        assertTrue(headerValues.hasNext());
        assertEquals(headerValues.next(), "headerValue3");
        assertTrue(headerValues.hasNext());
        assertEquals(headerValues.next(), "headerValue4");
        assertFalse(headerValues.hasNext());

        headerValues = headers.getHeaders("DummyHeader").iterator();
        assertFalse(headerValues.hasNext());
    }

    @Test
    public void charset_parsing() {
        assertEquals(null, Headers.extractQuotedValueFromHeader("text/html; other-data=\"charset=UTF-8\"", "charset"));
        assertEquals(null, Headers.extractQuotedValueFromHeader("text/html;", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=\"UTF-8\"", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=UTF-8", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=\"UTF-8\"; foo=bar", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=UTF-8 foo=bar", "charset"));
    }

    @Test
    public void extract_existing_boundary() {
        assertEquals("--xyz", Headers.extractBoundaryFromHeader("multipart/form-data; boundary=--xyz; param=abc"));
    }

    @Test
    public void extract_missing_boundary() {
        assertNull(Headers.extractBoundaryFromHeader("multipart/form-data; boundary;"));
    }

    @Test
    public void extract_param_with_tailing_whitespace() {
        assertEquals("abc", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=abc", "param"));
        assertEquals("abc", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=abc ", "param"));
        assertEquals("", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param= abc", "param"));
        assertEquals("", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param= abc ", "param"));

        assertEquals("abc", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\"abc\"", "param"));
        assertEquals("abc ", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\"abc \"", "param"));
        assertEquals(" abc", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\" abc\"", "param"));
        assertEquals(" abc ", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=\" abc \"", "param"));

        assertNull(Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param\t=abc", "param"));
        assertEquals("ab", Headers.extractQuotedValueFromHeader("multipart/form-data; boundary=--xyz; param=ab\tc", "param"));
    }
}
