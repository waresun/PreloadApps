package com.novoda.sexp.marshaller;

public interface AttributeMarshaller<T> {
    T marshal(String... input);
}
