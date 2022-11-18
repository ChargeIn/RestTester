package com.flop.resttester;

import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;

public class RestTesterState {

    @Tag("state")
    public Element state;

    public RestTesterState() {
        this.state = new Element("root");
    }
}