package com.parser.android;

public class AppItem {
    public final PkgName pkg;
    public final Link link;
    public final Title title;
    public final Icon icon;
    public AppItem (PkgName pkg, Link link, Title title, Icon icon) {
        this.pkg = pkg;
        this.link = link;
        this.title = title;
        this.icon = icon;
    }
}
