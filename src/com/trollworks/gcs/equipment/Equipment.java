/*
 * Copyright (c) 1998-2019 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, version 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, version 2.0.
 */

package com.trollworks.gcs.equipment;

import com.trollworks.gcs.app.GCSImages;
import com.trollworks.gcs.character.GURPSCharacter;
import com.trollworks.gcs.common.DataFile;
import com.trollworks.gcs.common.HasSourceReference;
import com.trollworks.gcs.common.LoadState;
import com.trollworks.gcs.feature.ContainedWeightReduction;
import com.trollworks.gcs.feature.Feature;
import com.trollworks.gcs.preferences.DisplayPreferences;
import com.trollworks.gcs.preferences.SheetPreferences;
import com.trollworks.gcs.skill.SkillDefault;
import com.trollworks.gcs.weapon.MeleeWeaponStats;
import com.trollworks.gcs.weapon.RangedWeaponStats;
import com.trollworks.gcs.weapon.WeaponStats;
import com.trollworks.gcs.widgets.outline.ListRow;
import com.trollworks.gcs.widgets.outline.RowEditor;
import com.trollworks.toolkit.io.xml.XMLReader;
import com.trollworks.toolkit.io.xml.XMLWriter;
import com.trollworks.toolkit.ui.image.StdImage;
import com.trollworks.toolkit.ui.widget.outline.Column;
import com.trollworks.toolkit.ui.widget.outline.Row;
import com.trollworks.toolkit.utility.I18n;
import com.trollworks.toolkit.utility.units.WeightUnits;
import com.trollworks.toolkit.utility.units.WeightValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** A piece of equipment. */
public class Equipment extends ListRow implements HasSourceReference {
    private static final int       CURRENT_VERSION          = 6;
    private static final int       EQUIPMENT_SPLIT_VERSION  = 6;
    private static final String    DEFAULT_LEGALITY_CLASS   = "4";
    /** The extension for Equipment lists. */
    public static final String     OLD_EQUIPMENT_EXTENSION  = "eqp";
    /** The XML tag used for items. */
    public static final String     TAG_EQUIPMENT            = "equipment";
    /** The XML tag used for containers. */
    public static final String     TAG_EQUIPMENT_CONTAINER  = "equipment_container";
    private static final String    ATTRIBUTE_EQUIPPED       = "equipped";
    private static final String    TAG_QUANTITY             = "quantity";
    private static final String    TAG_DESCRIPTION          = "description";
    private static final String    TAG_TECH_LEVEL           = "tech_level";
    private static final String    TAG_LEGALITY_CLASS       = "legality_class";
    private static final String    TAG_VALUE                = "value";
    private static final String    TAG_WEIGHT               = "weight";
    private static final String    TAG_REFERENCE            = "reference";
    /** The prefix used in front of all IDs for the equipment. */
    public static final String     PREFIX                   = GURPSCharacter.CHARACTER_PREFIX + "equipment.";
    /** The field ID for equipped/carried/not carried changes. */
    public static final String     ID_EQUIPPED              = PREFIX + "Equipped";
    /** The field ID for quantity changes. */
    public static final String     ID_QUANTITY              = PREFIX + "Quantity";
    /** The field ID for description changes. */
    public static final String     ID_DESCRIPTION           = PREFIX + "Description";
    /** The field ID for tech level changes. */
    public static final String     ID_TECH_LEVEL            = PREFIX + "TechLevel";
    /** The field ID for legality changes. */
    public static final String     ID_LEGALITY_CLASS        = PREFIX + "LegalityClass";
    /** The field ID for value changes. */
    public static final String     ID_VALUE                 = PREFIX + "Value";
    /** The field ID for weight changes. */
    public static final String     ID_WEIGHT                = PREFIX + "Weight";
    /** The field ID for extended value changes */
    public static final String     ID_EXTENDED_VALUE        = PREFIX + "ExtendedValue";
    /** The field ID for extended weight changes */
    public static final String     ID_EXTENDED_WEIGHT       = PREFIX + "ExtendedWeight";
    /** The field ID for page reference changes. */
    public static final String     ID_REFERENCE             = PREFIX + "Reference";
    /** The field ID for when the categories change. */
    public static final String     ID_CATEGORY              = PREFIX + "Category";
    /** The field ID for when the row hierarchy changes. */
    public static final String     ID_LIST_CHANGED          = PREFIX + "ListChanged";
    /** The field ID for when the equipment becomes or stops being a weapon. */
    public static final String     ID_WEAPON_STATUS_CHANGED = PREFIX + "WeaponStatus";
    private boolean                mEquipped;
    private int                    mQuantity;
    private String                 mDescription;
    private String                 mTechLevel;
    private String                 mLegalityClass;
    private double                 mValue;
    private WeightValue            mWeight;
    private double                 mExtendedValue;
    private WeightValue            mExtendedWeight;
    private String                 mReference;
    private ArrayList<WeaponStats> mWeapons;

    /**
     * Creates a new equipment.
     *
     * @param dataFile    The data file to associate it with.
     * @param isContainer Whether or not this row allows children.
     */
    public Equipment(DataFile dataFile, boolean isContainer) {
        super(dataFile, isContainer);
        mEquipped       = true;
        mQuantity       = 1;
        mDescription    = I18n.Text("Equipment");
        mTechLevel      = "";
        mLegalityClass  = DEFAULT_LEGALITY_CLASS;
        mReference      = "";
        mWeight         = new WeightValue(0, DisplayPreferences.getWeightUnits());
        mExtendedWeight = new WeightValue(mWeight);
        mWeapons        = new ArrayList<>();
    }

    /**
     * Creates a clone of an existing equipment and associates it with the specified data file.
     *
     * @param dataFile  The data file to associate it with.
     * @param equipment The equipment to clone.
     * @param deep      Whether or not to clone the children, grandchildren, etc.
     */
    public Equipment(DataFile dataFile, Equipment equipment, boolean deep) {
        super(dataFile, equipment);
        boolean forSheet = dataFile instanceof GURPSCharacter;
        mEquipped       = forSheet ? equipment.mEquipped : true;
        mQuantity       = forSheet ? equipment.mQuantity : 1;
        mDescription    = equipment.mDescription;
        mTechLevel      = equipment.mTechLevel;
        mLegalityClass  = equipment.mLegalityClass;
        mValue          = equipment.mValue;
        mWeight         = new WeightValue(equipment.mWeight);
        mExtendedValue  = mQuantity * mValue;
        mExtendedWeight = new WeightValue(mWeight);
        mExtendedWeight.setValue(mExtendedWeight.getValue() * mQuantity);
        mReference = equipment.mReference;
        mWeapons   = new ArrayList<>(equipment.mWeapons.size());
        for (WeaponStats weapon : equipment.mWeapons) {
            if (weapon instanceof MeleeWeaponStats) {
                mWeapons.add(new MeleeWeaponStats(this, (MeleeWeaponStats) weapon));
            } else if (weapon instanceof RangedWeaponStats) {
                mWeapons.add(new RangedWeaponStats(this, (RangedWeaponStats) weapon));
            }
        }
        if (deep) {
            int count = equipment.getChildCount();

            for (int i = 0; i < count; i++) {
                addChild(new Equipment(dataFile, (Equipment) equipment.getChild(i), true));
            }
        }
    }

    /**
     * Loads an equipment and associates it with the specified data file.
     *
     * @param dataFile The data file to associate it with.
     * @param reader   The XML reader to load from.
     * @param state    The {@link LoadState} to use.
     */
    public Equipment(DataFile dataFile, XMLReader reader, LoadState state) throws IOException {
        this(dataFile, TAG_EQUIPMENT_CONTAINER.equals(reader.getName()));
        load(reader, state);
    }

    @Override
    public boolean isEquivalentTo(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Equipment && super.isEquivalentTo(obj)) {
            Equipment row = (Equipment) obj;
            if (mQuantity == row.mQuantity && mValue == row.mValue && mEquipped == row.mEquipped && mWeight.equals(row.mWeight) && mDescription.equals(row.mDescription) && mTechLevel.equals(row.mTechLevel) && mLegalityClass.equals(row.mLegalityClass) && mReference.equals(row.mReference)) {
                return mWeapons.equals(row.mWeapons);
            }
        }
        return false;
    }

    @Override
    public String getLocalizedName() {
        return I18n.Text("Equipment");
    }

    @Override
    public String getListChangedID() {
        return ID_LIST_CHANGED;
    }

    @Override
    public String getXMLTagName() {
        return canHaveChildren() ? TAG_EQUIPMENT_CONTAINER : TAG_EQUIPMENT;
    }

    @Override
    public int getXMLTagVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public String getRowType() {
        return I18n.Text("Equipment");
    }

    @Override
    protected void prepareForLoad(LoadState state) {
        super.prepareForLoad(state);
        mEquipped      = true;
        mQuantity      = 1;
        mDescription   = I18n.Text("Equipment");
        mTechLevel     = "";
        mLegalityClass = DEFAULT_LEGALITY_CLASS;
        mReference     = "";
        mValue         = 0.0;
        mWeight.setValue(0.0);
        mWeapons = new ArrayList<>();
    }

    @Override
    protected void loadAttributes(XMLReader reader, LoadState state) {
        super.loadAttributes(reader, state);
        if (mDataFile instanceof GURPSCharacter) {
            if (state.mDataItemVersion == 0 || state.mDataItemVersion >= EQUIPMENT_SPLIT_VERSION) {
                mEquipped = reader.isAttributeSet(ATTRIBUTE_EQUIPPED);
            } else {
                mEquipped = "equipped".equals(reader.getAttribute("state"));
            }
            if (state.mDataFileVersion < GURPSCharacter.SEPARATED_EQUIPMENT_VERSION) {
                if (!mEquipped && !"carried".equals(reader.getAttribute("state"))) {
                    state.mUncarriedEquipment.add(this);
                }
            }
        }
    }

    @Override
    protected void loadSubElement(XMLReader reader, LoadState state) throws IOException {
        String name = reader.getName();
        if (TAG_DESCRIPTION.equals(name)) {
            mDescription = reader.readText().replace("\n", " ");
        } else if (TAG_TECH_LEVEL.equals(name)) {
            mTechLevel = reader.readText().replace("\n", " ");
        } else if (TAG_LEGALITY_CLASS.equals(name)) {
            mLegalityClass = reader.readText().replace("\n", " ");
        } else if (TAG_VALUE.equals(name)) {
            mValue = reader.readDouble(0.0);
        } else if (TAG_WEIGHT.equals(name)) {
            mWeight = WeightValue.extract(reader.readText(), false);
        } else if (TAG_REFERENCE.equals(name)) {
            mReference = reader.readText().replace("\n", " ");
        } else if (!state.mForUndo && (TAG_EQUIPMENT.equals(name) || TAG_EQUIPMENT_CONTAINER.equals(name))) {
            addChild(new Equipment(mDataFile, reader, state));
        } else if (MeleeWeaponStats.TAG_ROOT.equals(name)) {
            mWeapons.add(new MeleeWeaponStats(this, reader));
        } else if (RangedWeaponStats.TAG_ROOT.equals(name)) {
            mWeapons.add(new RangedWeaponStats(this, reader));
        } else if (!canHaveChildren()) {
            if (TAG_QUANTITY.equals(name)) {
                mQuantity = reader.readInteger(1);
            } else {
                super.loadSubElement(reader, state);
            }
        } else {
            super.loadSubElement(reader, state);
        }
    }

    @Override
    protected void finishedLoading(LoadState state) {
        updateExtendedValue(false);
        updateExtendedWeight(false);
        super.finishedLoading(state);
    }

    @Override
    protected void saveAttributes(XMLWriter out, boolean forUndo) {
        if (mDataFile instanceof GURPSCharacter) {
            out.writeAttribute(ATTRIBUTE_EQUIPPED, mEquipped);
        }
    }

    @Override
    protected void saveSelf(XMLWriter out, boolean forUndo) {
        if (!canHaveChildren()) {
            out.simpleTag(TAG_QUANTITY, mQuantity);
        }
        out.simpleTagNotEmpty(TAG_DESCRIPTION, mDescription);
        out.simpleTagNotEmpty(TAG_TECH_LEVEL, mTechLevel);
        out.simpleTagNotEmpty(TAG_LEGALITY_CLASS, mLegalityClass);
        out.simpleTag(TAG_VALUE, mValue);
        if (mWeight.getNormalizedValue() != 0) {
            out.simpleTag(TAG_WEIGHT, mWeight.toString(false));
        }
        out.simpleTagNotEmpty(TAG_REFERENCE, mReference);
        for (WeaponStats weapon : mWeapons) {
            weapon.save(out);
        }
    }

    @Override
    public void update() {
        updateExtendedValue(true);
        updateExtendedWeight(true);
    }

    public void updateNoNotify() {
        updateExtendedValue(false);
        updateExtendedWeight(false);
    }

    /** @return The quantity. */
    public int getQuantity() {
        return mQuantity;
    }

    /**
     * @param quantity The quantity to set.
     * @return Whether it was modified.
     */
    public boolean setQuantity(int quantity) {
        if (quantity != mQuantity) {
            mQuantity = quantity;
            startNotify();
            notify(ID_QUANTITY, this);
            updateContainingWeights(true);
            updateContainingValues(true);
            endNotify();
            return true;
        }
        return false;
    }

    /** @return The description. */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @param description The description to set.
     * @return Whether it was modified.
     */
    public boolean setDescription(String description) {
        if (!mDescription.equals(description)) {
            mDescription = description;
            notifySingle(ID_DESCRIPTION);
            return true;
        }
        return false;
    }

    /** @return The tech level. */
    public String getTechLevel() {
        return mTechLevel;
    }

    /**
     * @param techLevel The tech level to set.
     * @return Whether it was modified.
     */
    public boolean setTechLevel(String techLevel) {
        if (!mTechLevel.equals(techLevel)) {
            mTechLevel = techLevel;
            notifySingle(ID_TECH_LEVEL);
            return true;
        }
        return false;
    }

    /** @return The legality class. */
    public String getLegalityClass() {
        return mLegalityClass;
    }

    public String getDisplayLegalityClass() {
        String lc = getLegalityClass().trim();
        switch (lc) {
        case "0":
            return I18n.Text("LC0: Banned");
        case "1":
            return I18n.Text("LC1: Military");
        case "2":
            return I18n.Text("LC2: Restricted");
        case "3":
            return I18n.Text("LC3: Licensed");
        case "4":
            return I18n.Text("LC4: Open");
        default:
            return lc;
        }
    }

    /**
     * @param legalityClass The legality class to set.
     * @return Whether it was modified.
     */
    public boolean setLegalityClass(String legalityClass) {
        if (!mLegalityClass.equals(legalityClass)) {
            mLegalityClass = legalityClass;
            notifySingle(ID_LEGALITY_CLASS);
            return true;
        }
        return false;
    }

    /** @return The value. */
    public double getValue() {
        return mValue;
    }

    /**
     * @param value The value to set.
     * @return Whether it was modified.
     */
    public boolean setValue(double value) {
        if (value != mValue) {
            mValue = value;
            startNotify();
            notify(ID_VALUE, this);
            updateContainingValues(true);
            endNotify();
            return true;
        }
        return false;
    }

    /** @return The extended value. */
    public double getExtendedValue() {
        return mExtendedValue;
    }

    /** @return The weight. */
    public WeightValue getWeight() {
        return mWeight;
    }

    /**
     * @param weight The weight to set.
     * @return Whether it was modified.
     */
    public boolean setWeight(WeightValue weight) {
        if (!mWeight.equals(weight)) {
            mWeight = new WeightValue(weight);
            startNotify();
            notify(ID_WEIGHT, this);
            updateContainingWeights(true);
            endNotify();
            return true;
        }
        return false;
    }

    private boolean updateExtendedWeight(boolean okToNotify) {
        WeightValue saved = mExtendedWeight;
        int         count = getChildCount();
        WeightUnits units = mWeight.getUnits();
        mExtendedWeight = new WeightValue(mWeight.getValue() * mQuantity, units);
        WeightValue contained = new WeightValue(0, units);
        for (int i = 0; i < count; i++) {
            Equipment   one    = (Equipment) getChild(i);
            WeightValue weight = one.mExtendedWeight;
            if (SheetPreferences.areGurpsMetricRulesUsed()) {
                if (units.isMetric()) {
                    weight = GURPSCharacter.convertToGurpsMetric(weight);
                } else {
                    weight = GURPSCharacter.convertFromGurpsMetric(weight);
                }
            }
            contained.add(weight);
        }
        int         percentage = 0;
        WeightValue reduction  = new WeightValue(0, units);
        for (Feature feature : getFeatures()) {
            if (feature instanceof ContainedWeightReduction) {
                ContainedWeightReduction cwr = (ContainedWeightReduction) feature;
                if (cwr.isPercentage()) {
                    percentage += cwr.getPercentageReduction();
                } else {
                    reduction.add(cwr.getAbsoluteReduction());
                }
            }
        }
        if (percentage > 0) {
            if (percentage >= 100) {
                contained = new WeightValue(0, units);
            } else {
                contained.subtract(new WeightValue(contained.getNormalizedValue() * percentage / 100, units));
            }
        }
        contained.subtract(reduction);
        if (contained.getNormalizedValue() > 0) {
            mExtendedWeight.add(contained);
        }
        if (!saved.equals(mExtendedWeight)) {
            if (okToNotify) {
                notify(ID_EXTENDED_WEIGHT, this);
            }
            return true;
        }
        return false;
    }

    private void updateContainingWeights(boolean okToNotify) {
        Row parent = this;
        while (parent != null && parent instanceof Equipment) {
            Equipment parentRow = (Equipment) parent;
            if (parentRow.updateExtendedWeight(okToNotify)) {
                parent = parentRow.getParent();
            } else {
                break;
            }
        }
    }

    private boolean updateExtendedValue(boolean okToNotify) {
        double savedValue = mExtendedValue;
        int    count      = getChildCount();
        mExtendedValue = mQuantity * mValue;
        for (int i = 0; i < count; i++) {
            Equipment child = (Equipment) getChild(i);
            mExtendedValue += child.mExtendedValue;
        }
        if (savedValue != mExtendedValue) {
            if (okToNotify) {
                notify(ID_EXTENDED_VALUE, this);
            }
            return true;
        }
        return false;
    }

    private void updateContainingValues(boolean okToNotify) {
        Row parent = this;
        while (parent != null && parent instanceof Equipment) {
            Equipment parentRow = (Equipment) parent;
            if (parentRow.updateExtendedValue(okToNotify)) {
                parent = parentRow.getParent();
            } else {
                break;
            }
        }
    }

    /** @return The extended weight. */
    public WeightValue getExtendedWeight() {
        return mExtendedWeight;
    }

    /** @return Whether this item is equipped. */
    public boolean isEquipped() {
        return mEquipped;
    }

    /**
     * @param equipped The new equipped state.
     * @return Whether it was changed.
     */
    public boolean setEquipped(boolean equipped) {
        if (mEquipped != equipped) {
            mEquipped = equipped;
            notifySingle(ID_EQUIPPED);
            return true;
        }
        return false;
    }

    @Override
    public String getReference() {
        return mReference;
    }

    @Override
    public String getReferenceHighlight() {
        return getDescription();
    }

    @Override
    public boolean setReference(String reference) {
        if (!mReference.equals(reference)) {
            mReference = reference;
            notifySingle(ID_REFERENCE);
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(String text, boolean lowerCaseOnly) {
        if (getDescription().toLowerCase().indexOf(text) != -1) {
            return true;
        }
        return super.contains(text, lowerCaseOnly);
    }

    @Override
    public Object getData(Column column) {
        return EquipmentColumn.values()[column.getID()].getData(this);
    }

    @Override
    public String getDataAsText(Column column) {
        return EquipmentColumn.values()[column.getID()].getDataAsText(this);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /** @return The weapon list. */
    public List<WeaponStats> getWeapons() {
        return Collections.unmodifiableList(mWeapons);
    }

    /**
     * @param weapons The weapons to set.
     * @return Whether it was modified.
     */
    public boolean setWeapons(List<WeaponStats> weapons) {
        if (!mWeapons.equals(weapons)) {
            mWeapons = new ArrayList<>(weapons);
            for (WeaponStats weapon : mWeapons) {
                weapon.setOwner(this);
            }
            notifySingle(ID_WEAPON_STATUS_CHANGED);
            return true;
        }
        return false;
    }

    @Override
    public StdImage getIcon(boolean large) {
        return GCSImages.getEquipmentIcons().getImage(large ? 64 : 16);
    }

    @Override
    public RowEditor<? extends ListRow> createEditor() {
        return new EquipmentEditor(this, getOwner().getProperty(EquipmentList.TAG_OTHER_ROOT) == null);
    }

    @Override
    public void fillWithNameableKeys(HashSet<String> set) {
        super.fillWithNameableKeys(set);
        extractNameables(set, mDescription);
        for (WeaponStats weapon : mWeapons) {
            for (SkillDefault one : weapon.getDefaults()) {
                one.fillWithNameableKeys(set);
            }
        }
    }

    @Override
    public void applyNameableKeys(HashMap<String, String> map) {
        super.applyNameableKeys(map);
        mDescription = nameNameables(map, mDescription);
        for (WeaponStats weapon : mWeapons) {
            for (SkillDefault one : weapon.getDefaults()) {
                one.applyNameableKeys(map);
            }
        }
    }

    @Override
    protected String getCategoryID() {
        return ID_CATEGORY;
    }

    @Override
    public String getToolTip(Column column) {
        return EquipmentColumn.values()[column.getID()].getToolTip(this);
    }
}
