package com.crazyma;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.crazyma.form.AndroidSelectorDialog;

import java.util.Locale;

/**
 * Created by david on 2016/7/18.
 */
public class CreateImageDrawable extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        final VirtualFile dir = anActionEvent.getData(LangDataKeys.VIRTUAL_FILE);
        if (dir == null) {
            String title = "Cannot create files";
            String msg = String.format(Locale.US,"Oops! Something goes wrong >w<");
            Messages.showMessageDialog(
                    anActionEvent.getProject(), msg, title, Messages.getErrorIcon());
            return;
        }

        VirtualFile resDir = getResDir(dir);

        if(isSelectedFileValid(dir) && resDir!= null){
            Project project = anActionEvent.getProject();
            AndroidSelectorDialog dialog = new AndroidSelectorDialog(project, resDir, dir);
            dialog.show();
        }else{
            String title = "Cannot create files";
            String msg = String.format(Locale.US,"Oops! Something goes wrong >w<");
            Messages.showMessageDialog(
                    anActionEvent.getProject(), msg, title, Messages.getErrorIcon());

        }

    }

    private boolean isSelectedFileValid(VirtualFile dir){
        String regex = ".*((\\.png)|(\\.jpg))|(\\.9.png)$";
        return dir.getName().matches(regex);
    }

    private VirtualFile getResDir(VirtualFile dir){
        VirtualFile resDir;
        do{
            resDir = dir.getParent();
            dir = resDir;
        }while(resDir.getName().contains("drawable"));

        if(resDir != null && resDir.getName().equals("res"))
            return resDir;
        return null;
    }
}
