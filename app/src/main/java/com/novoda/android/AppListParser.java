package com.novoda.android;

import com.novoda.sax.Element;
import com.novoda.sax.ElementListener;
import com.novoda.sexp.finder.ElementFinder;
import com.novoda.sexp.finder.ElementFinderFactory;
import com.novoda.sexp.parser.ParseWatcher;
import com.novoda.sexp.parser.Parser;

import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

public class AppListParser implements Parser<AppListItem> {

    private static final String TAG_ITEM = "item";

    private final ElementFinder<AppItem> appListItemElementFinder;

    private ParseWatcher<AppListItem> listener;
    private AppListHolder appListHolder;

    public AppListParser(ElementFinderFactory factory) {
        this.appListItemElementFinder = factory.getListElementFinder(new AppItemParser(factory), parseWatcher);
    }

    private final ParseWatcher<AppItem> parseWatcher = new ParseWatcher<AppItem>() {
        @Override
        public void onParsed(AppItem item) {
            appListHolder.appItems.add(item);
        }
    };

    @Override
    public void parse(Element element, final ParseWatcher<AppListItem> listener) {
        this.listener = listener;
        element.setElementListener(channelParseListener);
        appListItemElementFinder.find(element, TAG_ITEM);
    }

    private final ElementListener channelParseListener = new ElementListener() {
        @Override
        public void start(Attributes attributes) {
            appListHolder = new AppListHolder();
        }

        @Override
        public void end() {
            listener.onParsed(appListHolder.asChannel());
        }
    };

    private static class AppListHolder {
        private final List<AppItem> appItems = new ArrayList<AppItem>();

        public AppListItem asChannel() {
            return new AppListItem(appItems);
        }
    }
}
