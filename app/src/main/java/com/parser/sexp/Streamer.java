package com.parser.sexp;

import com.parser.sax.RootElement;

/**
 * This streamer can be used when you want to return your parsed object synchronously.
 * A good example being {@link SimpleEasyXmlParser#parse(String, Streamer)}
 *
 * @param <R> the type of object you expect as a result of xml parsing
 */
public interface Streamer<R> {
    /**
     * @return The root tag of your XML file
     */
    RootTag getRootTag();

    /**
     * This is where you stream the xml 'tree' by using
     * {@link com.parser.sexp.finder.ElementFinder ElementFinder}
     * or {@link com.parser.sexp.parser.Parser Parser}
     * objects to parse the XML.<br/><br/>
     * You can use the {@link com.parser.sexp.finder.ElementFinderFactory ElementFinderFactory}
     * to create tree crawlers, or you can create your own if you implement one of:
     * <ul>
     * <li>{@link com.parser.sexp.finder.ElementFinder ElementFinder}</li>
     * <li>{@link com.parser.sexp.marshaller.AttributeMarshaller AttributeMarshaller}</li>
     * <li>{@link com.parser.sexp.marshaller.BodyMarshaller BodyMarshaller}</li>
     * <li>{@link com.parser.sexp.parser.Parser Parser}</li>
     * <li>{@link com.parser.sexp.parser.ParseWatcher ParseWatcher}</li>
     * <li>{@link com.parser.sexp.parser.ListParseWatcher ListParseWatcher}</li>
     * </ul>
     *
     * @param rootElement the root element of your XML file
     */
    void stream(RootElement rootElement);

    /**
     * @return the object you expected to be streamed
     */
    R getStreamResult();
}
