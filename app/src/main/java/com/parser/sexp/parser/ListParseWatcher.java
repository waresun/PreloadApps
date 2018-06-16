package com.parser.sexp.parser;

import com.parser.sax.ElementListener;

import org.xml.sax.Attributes;

public interface ListParseWatcher<T> extends ParseWatcher<T>, ElementListener {
    @Override
    void start(Attributes attributes);

    @Override
    void onParsed(T item);

    @Override
    void end();
}
