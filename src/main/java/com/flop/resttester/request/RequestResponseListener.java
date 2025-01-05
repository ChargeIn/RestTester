/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.response.ResponseData;

import java.util.EventListener;

public interface RequestResponseListener extends EventListener {
    void onRequestResponse(ResponseData data);
}
