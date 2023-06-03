/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.utils;

import java.util.Timer;
import java.util.TimerTask;

public class Debouncer {

    private final Timer timer;
    private TimerTask lastTask = null;
    private final DebounceCallback callback;
    private long delay = 300;


    public Debouncer(DebounceCallback callback, long delay) {
        this.callback = callback;
        this.timer = new Timer(true); //run as daemon
        this.delay = delay;
    }

    public Debouncer(DebounceCallback callback) {
        this.callback = callback;
        this.timer = new Timer(true); //run as daemon
    }

    public void debounce() {
        if (this.delay < 0) return;

        this.cancelPreviousTasks(); //if any

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Debouncer.this.callback.execute();
                Debouncer.this.cancelPreviousTasks();
            }
        };

        this.scheduleNewTask(timerTask, this.delay);
    }

    private void cancelPreviousTasks() {
        if (this.lastTask != null) {
            this.lastTask.cancel();
            this.lastTask = null;
        }
    }

    private void scheduleNewTask(TimerTask timerTask, long delay) {
        this.timer.schedule(timerTask, delay);
        this.lastTask = timerTask;
    }
}