/*
 * Copyright ©1998-2020 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, version 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, version 2.0.
 */

package com.trollworks.gcs.menu.library;

import com.trollworks.gcs.library.Library;
import com.trollworks.gcs.library.LibraryUpdater;
import com.trollworks.gcs.menu.Command;
import com.trollworks.gcs.menu.StdMenuBar;
import com.trollworks.gcs.ui.MarkdownDocument;
import com.trollworks.gcs.ui.widget.WindowUtils;
import com.trollworks.gcs.utility.I18n;
import com.trollworks.gcs.utility.Release;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class LibraryUpdateCommand extends Command {
    private Library mLibrary;

    public LibraryUpdateCommand(Library library) {
        super(library.getTitle(), "lib:" + library.getKey());
        mLibrary = library;
    }

    @Override
    public void adjust() {
        Release availableUpgrade = mLibrary.getAvailableUpgrade();
        setTitle(availableUpgrade == null ? String.format(I18n.Text("%s is up to date"), mLibrary.getTitle()) : String.format(I18n.Text("Update %s to v%s"), mLibrary.getTitle(), availableUpgrade.getVersion().toString()));
        if (StdMenuBar.SUPRESS_MENUS) {
            setEnabled(false);
            return;
        }
        setEnabled(availableUpgrade != null);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Release availableUpgrade = mLibrary.getAvailableUpgrade();
        if (availableUpgrade != null) {
            askUserToUpdate(mLibrary, availableUpgrade);
        }
    }

    public static void askUserToUpdate(Library library, Release release) {
        JTextPane   markdown = new JTextPane(new MarkdownDocument(I18n.Text("Existing content for this library will be removed and replaced. Content in other libraries will not be modified.\n\n" + release.getNotes())));
        Dimension   size     = markdown.getPreferredSize();
        JScrollPane scroller = new JScrollPane(markdown);
        int         maxWidth = Math.min(600, WindowUtils.getMaximumWindowBounds().width * 3 / 2);
        if (size.width > maxWidth) {
            markdown.setSize(new Dimension(maxWidth, Short.MAX_VALUE));
            size = markdown.getPreferredSize();
            size.width = maxWidth;
            markdown.setPreferredSize(size);
        }
        String update = I18n.Text("Update");
        if (WindowUtils.showOptionDialog(null, scroller, String.format(I18n.Text("%s v%s is available!"), library.getTitle(), release.getVersion()), true, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{update, I18n.Text("Ignore")}, update) == JOptionPane.OK_OPTION) {
            LibraryUpdater.download(library, release);
        }
    }
}