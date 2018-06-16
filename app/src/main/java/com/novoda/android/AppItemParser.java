package com.novoda.android;

import com.novoda.sax.Element;
import com.novoda.sax.ElementListener;
import com.novoda.sexp.finder.ElementFinder;
import com.novoda.sexp.finder.ElementFinderFactory;
import com.novoda.sexp.parser.ParseWatcher;
import com.novoda.sexp.parser.Parser;

import org.xml.sax.Attributes;

public class AppItemParser implements Parser<AppItem> {
    private static final String TAG_TITLE = "title";
    private static final String TAG_PKG = "pkg";
    private static final String TAG_LINK = "link";
    private static final String TAG_ICON = "icon";

    private final ElementFinder<PkgName> pkgNameElementFinder;
    private final ElementFinder<Icon> iconElementFinder;
    private final ElementFinder<Title> titleElementFinder;
    private final ElementFinder<Link> linkElementFinder;

    private AppItemHolder itemHolder;
    private ParseWatcher<AppItem> listener;

    public AppItemParser(ElementFinderFactory factory) {
        this.titleElementFinder = factory.getStringWrapperTypeFinder(Title.class);
        this.iconElementFinder = factory.getStringWrapperTypeFinder(Icon.class);
        this.linkElementFinder = factory.getStringWrapperTypeFinder(Link.class);
        this.pkgNameElementFinder = factory.getStringWrapperTypeFinder(PkgName.class);
    }

    @Override
    public void parse(Element element, final ParseWatcher<AppItem> listener) {
        this.listener = listener;
        element.setElementListener(itemParseListener);
        titleElementFinder.find(element, TAG_TITLE);
        iconElementFinder.find(element, TAG_ICON);
        linkElementFinder.find(element, TAG_LINK);
        pkgNameElementFinder.find(element, TAG_PKG);
    }

    private final ElementListener itemParseListener = new ElementListener() {
        @Override
        public void start(Attributes attributes) {
            itemHolder = new AppItemHolder();
        }

        @Override
        public void end() {
            itemHolder.title = titleElementFinder.getResultOrThrow();
            itemHolder.icon = iconElementFinder.getResultOrThrow();
            itemHolder.link = linkElementFinder.getResultOrThrow();
            itemHolder.pkgName = pkgNameElementFinder.getResultOrThrow();

            listener.onParsed(itemHolder.asItem());
        }
    };

    private static class AppItemHolder {
        Title title;
        PkgName pkgName;
        Link link;
        Icon icon;

        public AppItem asItem() {
            return new AppItem(pkgName, link, title, icon);
        }
    }
}
