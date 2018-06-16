package com.parser.sexp.parser;

public interface ParseWatcher<T> {
    void onParsed(T item);
}
