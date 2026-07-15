package io.github.cezarywenta.carrental.adapter.web;

/**
 * Raised by the web adapter itself (malformed path variable, unsupported
 * status transition) as opposed to IllegalArgumentException thrown by
 * domain/application validation. Keeping it distinct means a future
 * narrowing of exception handling won't have to guess which
 * IllegalArgumentExceptions originated from deliberate adapter-level checks.
 */
final class InvalidRequestException extends RuntimeException {

    InvalidRequestException(String message) {
        super(message);
    }
}
