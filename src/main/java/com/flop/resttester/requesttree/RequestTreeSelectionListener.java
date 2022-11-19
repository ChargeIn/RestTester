package com.flop.resttester.requesttree;

import java.util.EventListener;

public interface RequestTreeSelectionListener extends EventListener {
    public void valueChanged(RequestTreeNodeData node);
}
