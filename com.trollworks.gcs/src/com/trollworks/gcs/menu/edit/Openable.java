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

package com.trollworks.gcs.menu.edit;

/**
 * Focusable controls and windows that want to participate in {@link OpenItemCommand} processing
 * must implement this interface.
 */
public interface Openable {
    /** @return Whether the selection can be opened. */
    boolean canOpenSelection();

    /** Called to open the current selection. */
    void openSelection();
}