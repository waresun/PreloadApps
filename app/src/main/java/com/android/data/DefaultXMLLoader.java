package com.android.data;

import android.content.Context;

import com.parser.android.AppListItem;
import com.parser.android.AppListParser;
import com.parser.android.Icon;
import com.parser.android.Link;
import com.parser.android.PkgName;
import com.parser.android.Title;
import com.parser.sexp.Instigator;
import com.parser.sexp.RootTag;
import com.parser.sexp.SimpleEasyXmlParser;
import com.parser.sexp.SimpleTagInstigator;
import com.parser.sexp.finder.ElementFinder;
import com.parser.sexp.finder.ElementFinderFactory;
import com.parser.sexp.parser.ParseFinishWatcher;
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
