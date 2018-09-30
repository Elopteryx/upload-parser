/**
 * File upload extension module for Jax-RS.
 */
module com.github.elopteryx.upload.rs {
    requires javax.ws.rs.api;
    requires com.github.elopteryx.upload;
    exports com.github.elopteryx.upload.rs;
}