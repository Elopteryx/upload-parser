/**
 * The top level of the package hierarchy which contains
 * classes related to using the core library in a
 * Jax-Rs environment.
 *
 * <p>This library includes a lightweight integration of the
 * core package with the Jax-Rs environment. It has
 * a message body reader to inject upload related
 * parameters into the controller methods. The library uses
 * different interfaces than those found in the core library,
 * because the Jax-Rs specification does not support async
 * processing for the message body worker classes, therefore
 * several methods and classes would not work here.
 *
 * <p>The library also includes annotations to help injecting
 * part items into the controller methods and also to allow
 * defining size constraints.
 */
package com.elopteryx.paint.upload.rs;