/*
 * Copyright (c) 1998-2016 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * version 2.0. If a copy of the MPL was not distributed with this file, You
 * can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as defined
 * by the Mozilla Public License, version 2.0.
 */

package com.trollworks.gcs.character;

import com.trollworks.gcs.app.GCSFonts;
import com.trollworks.toolkit.annotation.Localize;
import com.trollworks.toolkit.ui.GraphicsUtilities;
import com.trollworks.toolkit.ui.TextDrawing;
import com.trollworks.toolkit.ui.border.BoxedDropShadowBorder;
import com.trollworks.toolkit.ui.widget.ActionPanel;
import com.trollworks.toolkit.utility.Localization;
import com.trollworks.toolkit.utility.text.Text;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;

import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/** The notes panel. */
public class NotesPanel extends ActionPanel {
	@Localize("Notes")
	@Localize(locale = "de", value = "Notizen")
	@Localize(locale = "ru", value = "Заметка")
	@Localize(locale = "es", value = "Notas")
	private static String		NOTES;
	@Localize("Double-click to edit")
	@Localize(locale = "de", value = "Doppelklicken, um zu bearbeiten")
	@Localize(locale = "ru", value = "Дважды щёлкните для редактирования")
	@Localize(locale = "es", value = "Dobleclic para editar")
	private static String		NOTES_TOOLTIP;
	@Localize("Notes (continued)")
	@Localize(locale = "de", value = "Notizen (Fortsetzung)")
	@Localize(locale = "ru", value = "Заметка (продолжение)")
	@Localize(locale = "es", value = "Notas (continuación)")
	private static String		NOTES_CONTINUED;

	static {
		Localization.initialize();
	}

	/** The default action command generated by this panel. */
	public static final String	CMD_EDIT_NOTES	= "EditNotes";	//$NON-NLS-1$
	private static final String	NEWLINE			= "\n";		//$NON-NLS-1$
	private String				mNotes;

	/**
	 * Creates a new {@link NotesPanel}.
	 *
	 * @param notes The notes to display.
	 * @param continued Whether to use the "continued" title or not.
	 */
	public NotesPanel(String notes, boolean continued) {
		super();
		setBorder(new CompoundBorder(new BoxedDropShadowBorder(UIManager.getFont(GCSFonts.KEY_LABEL), continued ? NOTES_CONTINUED : NOTES), new EmptyBorder(0, 2, 0, 2)));
		setAlignmentY(-1f);
		setEnabled(true);
		setOpaque(true);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setActionCommand(CMD_EDIT_NOTES);
		setToolTipText(NOTES_TOOLTIP);
		mNotes = Text.standardizeLineEndings(notes);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					notifyActionListeners();
				}
			}
		});
	}

	/** @param width The width to wrap at. */
	public void setWrapWidth(int width) {
		mNotes = TextDrawing.wrapToPixelWidth(UIManager.getFont(GCSFonts.KEY_NOTES), mNotes, width);
	}

	/**
	 * @param height The maximum height allowed.
	 * @return The remaining text, or <code>null</code> if there isn't any.
	 */
	public String setMaxHeight(int height) {
		StringBuilder buffer = new StringBuilder();
		Insets insets = getInsets();
		int lineHeight = TextDrawing.getPreferredSize(UIManager.getFont(GCSFonts.KEY_NOTES), "Mg").height; //$NON-NLS-1$
		StringTokenizer tokenizer = new StringTokenizer(mNotes, NEWLINE, true);
		boolean wasReturn = false;

		height -= insets.top + insets.bottom;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			if (NEWLINE.equals(token)) {
				if (wasReturn) {
					height -= lineHeight;
				}
				wasReturn = true;
			} else {
				height -= lineHeight;
				wasReturn = false;
			}
			buffer.append(token);
			if (height < lineHeight) {
				boolean hasMore = tokenizer.hasMoreTokens();

				if (hasMore && NEWLINE.equals(tokenizer.nextToken())) {
					buffer.append('\n');
					hasMore = tokenizer.hasMoreTokens();
				}
				if (hasMore) {
					String notes = mNotes.substring(buffer.length());

					mNotes = buffer.toString();
					return notes;
				}
				return null;
			}
		}
		return null;
	}

	@Override
	public Dimension getMinimumSize() {
		Insets insets = getInsets();
		int height = TextDrawing.getPreferredSize(UIManager.getFont(GCSFonts.KEY_NOTES), "Mg").height; //$NON-NLS-1$
		return new Dimension(insets.left + insets.right, height + insets.top + insets.bottom);
	}

	@Override
	public Dimension getPreferredSize() {
		Insets insets = getInsets();
		Dimension size = TextDrawing.getPreferredSize(UIManager.getFont(GCSFonts.KEY_NOTES), mNotes);
		size.width += insets.left + insets.right;
		size.height += insets.top + insets.bottom;
		Dimension minSize = getMinimumSize();
		if (minSize.width > size.width) {
			size.width = minSize.width;
		}
		if (minSize.height > size.height) {
			size.height = minSize.height;
		}
		return size;
	}

	/** @param notes The notes to display. */
	public void setNotes(String notes) {
		mNotes = notes;
		revalidate();
	}

	@Override
	protected void paintComponent(Graphics gc) {
		super.paintComponent(GraphicsUtilities.prepare(gc));
		gc.setFont(UIManager.getFont(GCSFonts.KEY_NOTES));
		Rectangle bounds = getBounds();
		Insets insets = getInsets();
		bounds.x = insets.left;
		bounds.y = insets.top;
		bounds.width -= insets.left + insets.right;
		bounds.height -= insets.top + insets.bottom;
		TextDrawing.draw(gc, bounds, mNotes, SwingConstants.TOP, SwingConstants.LEFT);
	}
}
