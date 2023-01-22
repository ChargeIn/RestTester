/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class RestTesterNotifier {
    public static void notifyError(@Nullable Project project,
                                   String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Rest Tester Notification Group")
                .createNotification(content, NotificationType.ERROR)
                .notify(project);
    }

    public static void notifyInfo(@Nullable Project project,
                                  String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Rest Tester Notification Group")
                .createNotification(content, NotificationType.INFORMATION)
                .notify(project);
    }
}
