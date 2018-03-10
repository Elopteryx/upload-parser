package com.github.elopteryx.upload.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.elopteryx.upload.PartStream;

import org.junit.jupiter.api.Test;

class PartStreamImplTest {

    @Test
    void it_should_return_the_correct_data() {
        String fileName = "r-" + System.currentTimeMillis();
        String fieldName = "r-" + System.currentTimeMillis();
        String contentType = "r-" + System.currentTimeMillis();
        Headers headers = new Headers();
        headers.addHeader(Headers.CONTENT_TYPE, contentType);
        PartStream partStream = new PartStreamImpl(fileName, fieldName, headers);
        assertEquals(fileName, partStream.getSubmittedFileName());
        assertEquals(fieldName, partStream.getName());
        assertEquals(contentType, partStream.getContentType());
        assertEquals(partStream.isFile(), (partStream.getSubmittedFileName() != null));
        assertFalse(partStream.isFinished());
    }

    @Test
    void invalid_file_names_are_not_allowed() {
        String fileName = "r-" + System.currentTimeMillis() + '\u0000';
        PartStream partStream = new PartStreamImpl(fileName, null, new Headers());
        assertThrows(IllegalArgumentException.class, partStream::getSubmittedFileName);
    }
}
