/**
 * The top level of the package hierarchy which contains the
 * public API of this library. Interfaces and classes here will
 * not be changed frequently.
 *
 * <p>The library has been designed to hide the implementation details
 * from the users. When using it the users should not
 * need to import from the internal package, using the public
 * classes and the exception classes should be more than enough.
 *
 * <p>The parser object provides full control over the uploading
 * process. The functional interfaces can be used to perform
 * operations on the part objects. Information about the process
 * is provided in the upload context object, which is available
 * in every stage of the process.
 *
 * <p>The classes here have been designed to work with servlets. There
 * are two kinds of parsing, you can do it asynchronously or in
 * a blocking way. For the former the servlet must support support
 * async mode. Both of them are incompatible with the multipart
 * API of the servlet specification. Using that makes the
 * servlet input stream unavailable for this library or any code
 * that is written by the users.
 */
package com.elopteryx.paint.upload;