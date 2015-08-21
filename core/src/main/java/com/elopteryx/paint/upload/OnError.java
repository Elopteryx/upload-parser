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

import java.io.IOException;
import javax.servlet.ServletException;

/**
 * A functional interface. An implementation of it must be passed in the
 * {@link UploadParser#onError(OnError)} onError} method to call it after an error occurs.
 */
@FunctionalInterface
public interface OnError {

    /**
     * The consumer function to implement.
     * @param context The upload context
     * @param throwable The error that occurred
     * @throws IOException If an error occurs with the IO
     * @throws ServletException If and error occurred with the servlet
     */
    void onError(UploadContext context, Throwable throwable) throws IOException, ServletException;
    
}
