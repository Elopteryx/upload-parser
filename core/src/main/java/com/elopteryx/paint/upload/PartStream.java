/*
 * Copyright (C) 2015 Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elopteryx.paint.upload;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * This interface represents a part item, which is being
 * streamed from the client. As the object is created
 * before the item finished uploading available information
 * is limited.
 */
public interface PartStream {

    /**
     * Returns the content type of this part, as it was submitted by
     * the client. 
     *
     * @return The content type of this part
     */
    @Nullable
    String getContentType();

    /**
     * Returns the name of this part, which equals the name of the form
     * field the part was selected for.
     *
     * @return The name of this part as a String
     */
    @Nullable
    String getName();

    /**
     * Returns the known size of this part. This means that the
     * returned value depends on how many bytes have been processed. When called
     * during the start of the part processing the returned value will be equal
     * to the specified buffer threshold or the actual part size if it's smaller
     * than that. When called during the end the returned value is always the
     * actual size, because by that time it has been fully processed.
     *
     * @return A long specifying the known size of this part, in bytes.
     */
    @Nonnegative
    long getKnownSize();

    /**
     * Returns the file name specified by the client or null if the
     * part is a normal form field.
     *
     * @return The submitted file name
     */
    @Nullable
    String getSubmittedFileName();

    /**
     * Determines whether or not this PartStream instance represents
     * a file item. If it's a normal form field then it will return false.
     * Consequently, if this returns true then the {@link PartStream#getSubmittedFileName}
     * will return with a non-null value and vice-versa.
     *
     * @return true if the instance represents an uploaded file;
     * false if it represents a simple form field.
     */
    boolean isFile();

    /**
     * Returns the value of the specified mime header as a String. If
     * the Part did not include a header of the specified name, this
     * method returns null. If there are multiple headers with the same name,
     * this method returns the first header in the part. The header name
     * is case insensitive. You can use this method with any request header.
     *
     * @param name a String specifying the header name
     * @return a String containing the value of the requested header, or
     * null if the part does not have a header of that name
     */
    @Nullable
    String getHeader(String name);

    /**
     * Returns the values of the part header with the given name.
     *
     * @param name the header name whose values to return
     * @return a (possibly empty) Collection of the values of the header with the given name
     */
    Collection<String> getHeaders(String name);

    /**
     * Returns the header names of this part.
     *
     * @return a (possibly empty) Collection of the header names of this Part
     */
    Collection<String> getHeaderNames();

}
