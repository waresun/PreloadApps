package com.parser.sexp;

import com.parser.sax.RootElement;
import com.parser.sexp.finder.ElementFinder;
import com.parser.sexp.parser.ParseFinishWatcher;

public abstract class SimpleTagInstigator implements Instigator {

    private final ElementFinder<?> elementFinder;
    private final String elementTag;
    private final ParseFinishWatcher parseFinishWatcher;

    public SimpleTagInstigator(ElementFinder<?> elementFinder, String elementTag, ParseFinishWatcher parseFinishWatcher) {
        this.elementFinder = elementFinder;
        this.elementTag = elementTag;
        this.parseFinishWatcher = parseFinishWatcher;
    }

    @Override
    public void create(RootElement element) {
        elementFinder.find(element, elementTag);
    }

    @Override
    public void end() {
        parseFinishWatcher.onFinish();
    }
}
