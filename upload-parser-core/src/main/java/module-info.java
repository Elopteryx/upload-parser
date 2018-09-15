/**
 * Async file upload library for servlets.
 */
module com.github.elopteryx.upload {
    requires javax.servlet.api;
    exports com.github.elopteryx.upload;
    exports com.github.elopteryx.upload.errors;
    exports com.github.elopteryx.upload.util;
    exports com.github.elopteryx.upload.internal to com.github.elopteryx.upload.rs;
}