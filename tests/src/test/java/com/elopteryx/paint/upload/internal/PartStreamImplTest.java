package com.elopteryx.paint.upload.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.elopteryx.paint.upload.PartStream;

import org.junit.Test;

public class PartStreamImplTest {

    @Test
    public void it_should_return_the_correct_data() {
        String fileName = "r-" + System.currentTimeMillis();
        String fieldName = "r-" + System.currentTimeMillis();
        String contentType = "r-" + System.currentTimeMillis();
        PartStreamHeaders headers = new PartStreamHeaders();
        headers.addHeader(PartStreamHeaders.CONTENT_TYPE, contentType);
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
        PartStream partStream = new PartStreamImpl(fileName, null, new PartStreamHeaders());
        partStream.getSubmittedFileName();
    }
}
