package com.parser.sexp.marshaller;

public class LongBodyMarshaller implements BodyMarshaller<Long> {
    @Override
    public Long marshal(String input) {
        return Long.valueOf(input);
    }
}
