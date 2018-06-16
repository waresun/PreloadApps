package com.parser.sexp.marshaller;

public interface AttributeMarshaller<T> {
    T marshal(String... input);
}
