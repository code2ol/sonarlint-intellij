/**
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.ui.tree;

import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ui.UIUtil;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.issue.IssuePointer;
import org.sonarlint.intellij.ui.nodes.FileNode;
import org.sonarlint.intellij.ui.nodes.IssueNode;

/**
 * Extends {@link Tree} to provide context data for actions and initialize it
 */
public class IssueTree extends Tree implements DataProvider {
  private final Project project;

  public IssueTree(Project project, TreeModel model) {
    super(model);
    this.project = project;
    init();
  }

  private void init() {
    UIUtil.setLineStyleAngled(this);
    this.setShowsRootHandles(true);
    this.setRootVisible(true);
    this.setCellRenderer(new IssueTreeCellRenderer());
    this.expandRow(0);

    DefaultActionGroup group = new DefaultActionGroup();
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
    group.addSeparator();
    group.add(ActionManager.getInstance().getAction(IdeActions.GROUP_VERSION_CONTROLS));
    group.addSeparator();
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EXPAND_ALL));
    PopupHandler.installPopupHandler(this, group, ActionPlaces.TODO_VIEW_POPUP, ActionManager.getInstance());

    EditSourceOnDoubleClickHandler.install(this);
    EditSourceOnEnterKeyHandler.install(this);
  }

  @Nullable @Override public Object getData(@NonNls String dataId) {
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      DefaultMutableTreeNode node = getSelectedNode();
      if (!(node instanceof IssueNode)) {
        return null;
      }
      IssuePointer issue = ((IssueNode) node).issue();
      int offset;

      RangeMarker range = issue.range();
      if (range != null) {
        offset = range.getStartOffset();
      } else {
        offset = 0;
      }
      return new OpenFileDescriptor(project, issue.psiFile().getVirtualFile(), offset);
    } else if (PlatformDataKeys.TREE_EXPANDER.is(dataId)) {
      return new DefaultTreeExpander(this);
    } else if (PlatformDataKeys.VIRTUAL_FILE.is(dataId)) {
      return getSelectedFile();
    } else if (PlatformDataKeys.PSI_FILE.is(dataId)) {
      VirtualFile file = getSelectedFile();
      if (file != null) {
        return PsiManager.getInstance(project).findFile(file);
      }
      return null;
    } else if (PlatformDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
      VirtualFile f = getSelectedFile();
      return f != null ? (new VirtualFile[] {f}) : null;
    }

    return null;
  }

  private VirtualFile getSelectedFile() {
    DefaultMutableTreeNode node = getSelectedNode();
    if (!(node instanceof FileNode)) {
      return null;
    }
    FileNode fileNode = (FileNode) node;
    return fileNode.file();
  }

  private DefaultMutableTreeNode getSelectedNode() {
    TreePath path = getSelectionPath();
    if (path == null) {
      return null;
    }
    return (DefaultMutableTreeNode) path.getLastPathComponent();
  }
}
