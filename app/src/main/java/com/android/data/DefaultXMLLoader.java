package com.android.data;

import android.content.Context;

import com.novoda.android.AppListItem;
import com.novoda.android.AppListParser;
import com.novoda.android.Icon;
import com.novoda.android.Link;
import com.novoda.android.PkgName;
import com.novoda.android.Title;
import com.novoda.sexp.Instigator;
import com.novoda.sexp.RootTag;
import com.novoda.sexp.SimpleEasyXmlParser;
import com.novoda.sexp.SimpleTagInstigator;
import com.novoda.sexp.finder.ElementFinder;
import com.novoda.sexp.finder.ElementFinderFactory;
import com.novoda.sexp.parser.ParseFinishWatcher;
import com.android.asustore.R;

public class DefaultXMLLoader {
    private static ElementFinder<AppListItem> elementFinder;
    private final Context mContext;
    private AppListItem appListItem;
    private ParseFinishWatcher finishWatcher = new ParseFinishWatcher() {
        @Override
        public void onFinish() {
            appListItem = elementFinder.getResultOrThrow();

        }
    };

    public static class AppListInstigator extends SimpleTagInstigator {

        public AppListInstigator(ElementFinder<?> elementFinder,
                                 ParseFinishWatcher parseFinishWatcher) {
            super(elementFinder, "applist", parseFinishWatcher);
        }

        @Override
        public RootTag getRootTag() {
            return RootTag.create("appstore");
        }
    }
    public DefaultXMLLoader(Context context) {
        mContext = context;
    }
    public AppListItem loadXML() {
        Icon icon = new Icon("preload");
        Title title = new Title("preload");
        PkgName pkgName = new PkgName("preload");
        Link link = new Link("Preload");
        ElementFinderFactory factory = SimpleEasyXmlParser.getElementFinderFactory();
        elementFinder = factory.getTypeFinder(new AppListParser(factory));
        Instigator instigator = new AppListInstigator(elementFinder, finishWatcher);
        SimpleEasyXmlParser.parse(mContext.getResources().openRawResource(R.raw.applist), instigator);
        return appListItem;
    }
}
