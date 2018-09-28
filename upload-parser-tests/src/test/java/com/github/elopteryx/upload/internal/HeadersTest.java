package com.github.elopteryx.upload.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Locale;

class HeadersTest {

    @Test
    void add_and_retrieve_headers() {
        final var headers = new Headers();
        headers.addHeader(Headers.CONTENT_DISPOSITION, "form-data; name=\"FileItem\"; filename=\"file1.txt\"");
        headers.addHeader(Headers.CONTENT_TYPE, "text/plain");
        headers.addHeader("TestHeader", "headerValue1");
        headers.addHeader("TestHeader", "headerValue2");
        headers.addHeader("TestHeader", "headerValue3");
        headers.addHeader("testheader", "headerValue4");

        final var headerNames = headers.getHeaderNames().iterator();
        assertEquals(Headers.CONTENT_DISPOSITION.toLowerCase(Locale.ENGLISH), headerNames.next());
        assertEquals(Headers.CONTENT_TYPE.toLowerCase(Locale.ENGLISH), headerNames.next());
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
    void charset_parsing() {
        assertNull(Headers.extractQuotedValueFromHeader("text/html; other-data=\"charset=UTF-8\"", "charset"));
        assertNull(Headers.extractQuotedValueFromHeader("text/html;", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=\"UTF-8\"", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=UTF-8", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=\"UTF-8\"; foo=bar", "charset"));
        assertEquals("UTF-8", Headers.extractQuotedValueFromHeader("text/html; charset=UTF-8 foo=bar", "charset"));
    }

    @Test
    void extract_existing_boundary() {
        assertEquals("--xyz", Headers.extractBoundaryFromHeader("multipart/form-data; boundary=--xyz; param=abc"));
    }

    @Test
    void extract_missing_boundary() {
        assertNull(Headers.extractBoundaryFromHeader("multipart/form-data; boundary;"));
    }

    @Test
    void extract_param_with_tailing_whitespace() {
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
