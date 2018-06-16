package com.parser.android;

public class Link {
    private final String uri;

    public Link(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return uri;
    }
}
