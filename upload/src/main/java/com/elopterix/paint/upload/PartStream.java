/*
 * Copyright (C) 2015- Adam Forgacs
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
package com.elopterix.paint.upload;

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
     * Gets the content type of this part
     *
     * @return The content type of this part
     */
    @Nullable
    String getContentType();

    /**
     * Gets the name of this part
     *
     * @return The name of this part as a String
     */
    @Nullable
    String getName();

    /**
     * Gets the file name specified by the client
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
     * Gets the values of the Part header with the given name.
     *
     * @param name the header name whose values to return
     * @return a (possibly empty) Collection of the values of the header with the given name
     */
    Collection<String> getHeaders(String name);

    /**
     * Gets the header names of this Part.
     *
     * @return a (possibly empty) Collection of the header names of this Part
     */
    Collection<String> getHeaderNames();

}
