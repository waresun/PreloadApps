package com.parser.sexp.marshaller;

public interface BodyMarshaller<T> {
    T marshal(String input);
}
