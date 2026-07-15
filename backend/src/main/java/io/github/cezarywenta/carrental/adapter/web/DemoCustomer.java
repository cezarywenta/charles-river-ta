package io.github.cezarywenta.carrental.adapter.web;

/**
 * Stands in for authentication: every request acts as this one fixed
 * customer. This is not authentication. A production system would derive
 * the customer id from an authenticated principal, never from client-supplied
 * or hardcoded data.
 */
final class DemoCustomer {

    static final String ID = "customer-123";

    private DemoCustomer() {
    }
}
