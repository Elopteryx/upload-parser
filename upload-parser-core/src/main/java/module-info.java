/**
 * Async file upload module for servlets.
 */
module com.github.elopteryx.upload {
    requires jakarta.servlet;
    exports com.github.elopteryx.upload;
    exports com.github.elopteryx.upload.errors;
    exports com.github.elopteryx.upload.util;
}