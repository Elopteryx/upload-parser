package com.github.elopteryx.upload.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.elopteryx.upload.PartStream;

import org.junit.Test;

public class PartStreamImplTest {

    @Test
    public void it_should_return_the_correct_data() {
        String fileName = "r-" + System.currentTimeMillis();
        String fieldName = "r-" + System.currentTimeMillis();
        String contentType = "r-" + System.currentTimeMillis();
        Headers headers = new Headers();
        headers.addHeader(Headers.CONTENT_TYPE, contentType);
        PartStream partStream = new PartStreamImpl(fileName, fieldName, headers);
        assertEquals(fileName, partStream.getSubmittedFileName());
        assertEquals(fieldName, partStream.getName());
        assertEquals(contentType, partStream.getContentType());
        assertTrue(partStream.isFile() == (partStream.getSubmittedFileName() != null));
        assertFalse(partStream.isFinished());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalid_file_names_are_not_allowed() {
        String fileName = "r-" + System.currentTimeMillis() + '\u0000';
        PartStream partStream = new PartStreamImpl(fileName, null, new Headers());
        partStream.getSubmittedFileName();
    }
}
