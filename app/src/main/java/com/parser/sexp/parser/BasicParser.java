package com.parser.sexp.parser;

import com.parser.sax.Element;
import com.parser.sax.EndTextElementListener;
import com.parser.sexp.marshaller.BodyMarshaller;

public class BasicParser<T> implements Parser<T> {

    private final BodyMarshaller<T> bodyMarshaller;

    public BasicParser(BodyMarshaller<T> bodyMarshaller) {
        this.bodyMarshaller = bodyMarshaller;
    }

    @Override
    public void parse(Element element, final ParseWatcher<T> listener) {
        element.setEndTextElementListener(
                new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        listener.onParsed(bodyMarshaller.marshal(body));
                    }
                }
        );
    }

}
