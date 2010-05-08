<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE helpset PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
        "http://java.sun.com/products/javahelp/helpset_2_0.dtd">
<helpset version="2.0">
    <title>RouteConverter - English Help</title>
    <maps>
        <homeID>top</homeID>
        <mapref location="Map.jhm"/>
    </maps>
    <view mergetype="javax.help.UniteAppendMerge">
        <name>toc</name>
        <label>Table Of Contents</label>
        <type>javax.help.TOCView</type>
        <data>TOC.xml</data>
    </view>
    <view>
        <name>search</name>
        <label>Search</label>
        <type>javax.help.SearchView</type>
        <data engine="com.sun.java.help.search.DefaultSearchEngine">JavaHelpSearch</data>
    </view>
    <presentation default="true">
        <name>main window</name>
        <size width="600" height="600"/>
        <location x="100" y="100"/>
        <title>RouteConverter Help</title>
        <toolbar>
            <helpaction>javax.help.BackAction</helpaction>
            <helpaction>javax.help.ForwardAction</helpaction>
            <helpaction image="homeicon">javax.help.HomeAction</helpaction>
        </toolbar>
    </presentation>
    <impl>
        <helpsetregistry helpbrokerclass="javax.help.DefaultHelpBroker"/>
        <viewerregistry viewertype="text/html" viewerclass="com.sun.java.help.impl.CustomKit"/>
    </impl>
</helpset>