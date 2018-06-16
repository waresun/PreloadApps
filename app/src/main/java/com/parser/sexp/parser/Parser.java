package com.parser.sexp.parser;

import com.parser.sax.Element;

public interface Parser<T> {
    void parse(Element element, ParseWatcher<T> listener);
}

