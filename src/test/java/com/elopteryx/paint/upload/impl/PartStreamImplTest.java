package com.elopteryx.paint.upload.impl;

import com.elopteryx.paint.upload.PartStream;
import org.junit.Test;

import static com.elopteryx.paint.upload.Randoms.randomString;
import static org.junit.Assert.*;

public class PartStreamImplTest {

    @Test
    public void it_should_return_the_correct_data() {
        String fileName = randomString();
        String fieldName = randomString();
        String contentType = randomString();
        PartStream partStream = new PartStreamImpl(fileName, fieldName, contentType, null);
        assertEquals(fileName, partStream.getSubmittedFileName());
        assertEquals(fieldName, partStream.getName());
        assertEquals(contentType, partStream.getContentType());
        assertTrue(partStream.isFile() == (partStream.getSubmittedFileName() != null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalid_file_names_are_not_allowed() {
        String fileName = randomString() + '\u0000';
        PartStream partStream = new PartStreamImpl(fileName, null, null, null);
        partStream.getSubmittedFileName();
    }
}
