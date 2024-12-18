/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.components.CustomActionButtonWithText;
import com.flop.resttester.state.RestTesterStateService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnvironmentSelector implements EnvChangeListener {
    CustomActionButtonWithText button;
    private final int id;

    AnAction[] actions = new AnAction[]{};
    ActionGroup menuAction = new ActionGroup("Rest Tester Env", true) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        }

        @Override
        public AnAction @NotNull [] getChildren(@Nullable AnActionEvent anActionEvent) {
            return EnvironmentSelector.this.actions;
        }
    };

    // Add non-breaking space as workaround to make the popup bigger
    AnAction creatEnvSelector = new AnAction(" Edit Environments…         ") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            EnvironmentSelector.this.createNewEnv();
        }
    };

    public EnvironmentSelector() {
        Presentation presentation = new Presentation("Default Environment");
        presentation.setIcon(AllIcons.Actions.ProjectDirectory);
        presentation.setPopupGroup(true);

        RestTesterStateService stateService = RestTesterStateService.getInstance();

        this.button = new CustomActionButtonWithText(this.menuAction, presentation, "Rest Tester Toolbar", new Dimension(80, 20));
        this.button.setMaximumSize(new Dimension(225, 28));
        this.button.setBorder(JBUI.Borders.emptyLeft(6));

        // need to happen at the end since we need the button reference
        this.id = stateService.addEnvChangeListener(this);
    }

    public ActionButtonWithText getButton() {
        return this.button;
    }

    @Override
    public void onStateChange() {
        var stateService = RestTesterStateService.getInstance();
        var envs = stateService.environments;
        List<AnAction> actions = stateService.environments.keySet().stream().map(key -> {
            var env = envs.get(key);

            // add a non-breaking space as a workaround to create padding between the name and the icon
            return (new AnAction(" " + env.name, "", AllIcons.Actions.ProjectDirectory) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    if (!Objects.equals(key, stateService.selectedEnvironment)) {
                        stateService.selectEnvironment(key);
                    }
                }
            });
        }).collect(Collectors.toList());


        actions.add(this.creatEnvSelector);

        var selectedEnv = envs.get(stateService.selectedEnvironment);
        this.button.getPresentation().setText(selectedEnv.name);
        this.actions = actions.toArray(new AnAction[0]);
    }

    public void createNewEnv() {
        var stateService = RestTesterStateService.getInstance();

        EnvironmentManagementDialog dialog = new EnvironmentManagementDialog(stateService.getSnapShot());
        if (dialog.showAndGet()) {
            var snapshot = dialog.getSnapshot();
            stateService.updateEnvironment(snapshot);
        }
    }
}
