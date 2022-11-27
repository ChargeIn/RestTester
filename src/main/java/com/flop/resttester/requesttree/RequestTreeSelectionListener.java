package com.flop.resttester.requesttree;

import java.util.EventListener;

public interface RequestTreeSelectionListener extends EventListener {
    void valueChanged(RequestTreeNodeData node);
}
