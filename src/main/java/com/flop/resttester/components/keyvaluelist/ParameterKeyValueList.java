/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import java.util.List;

public class ParameterKeyValueList extends KeyValueList {
    @Override
    String getKeyPlaceholder() {
        return "Parameter";
    }

    @Override
    String getValuePlaceholder() {
        return "Value";
    }

    @Override
    List<String> getKeyProposals() {
        return List.of();
    }

    @Override
    List<String> getValueProposals() {
        return List.of();
    }
}
