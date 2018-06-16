package com.parser.sexp;

import com.parser.sax.EndElementListener;
import com.parser.sax.RootElement;

/**
 * {@link SimpleTagInstigator } is an example implementation
 */
public interface Instigator extends EndElementListener {
    /**
     * @return The root tag of your XML file
     */
    RootTag getRootTag();

    /**
     * This is where you traverse the xml 'tree' by using
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
    void create(RootElement rootElement);

    /**
     * Called when the corresponding closing root tag of your XML file is found
     */
    @Override
    void end();
}
