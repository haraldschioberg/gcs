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

package com.trollworks.gcs.character;

import com.trollworks.gcs.advantage.Advantage;
import com.trollworks.gcs.advantage.AdvantageContainerType;
import com.trollworks.gcs.advantage.AdvantageList;
import com.trollworks.gcs.collections.FilteredIterator;
import com.trollworks.gcs.datafile.DataFile;
import com.trollworks.gcs.datafile.LoadState;
import com.trollworks.gcs.equipment.Equipment;
import com.trollworks.gcs.equipment.EquipmentList;
import com.trollworks.gcs.feature.AttributeBonusLimitation;
import com.trollworks.gcs.feature.Bonus;
import com.trollworks.gcs.feature.BonusAttributeType;
import com.trollworks.gcs.feature.CostReduction;
import com.trollworks.gcs.feature.Feature;
import com.trollworks.gcs.feature.SkillBonus;
import com.trollworks.gcs.feature.SpellBonus;
import com.trollworks.gcs.feature.WeaponBonus;
import com.trollworks.gcs.io.Log;
import com.trollworks.gcs.io.xml.XMLNodeType;
import com.trollworks.gcs.io.xml.XMLReader;
import com.trollworks.gcs.io.xml.XMLWriter;
import com.trollworks.gcs.modifier.AdvantageModifier;
import com.trollworks.gcs.modifier.EquipmentModifier;
import com.trollworks.gcs.notes.Note;
import com.trollworks.gcs.notes.NoteList;
import com.trollworks.gcs.preferences.DisplayPreferences;
import com.trollworks.gcs.preferences.OutputPreferences;
import com.trollworks.gcs.preferences.SheetPreferences;
import com.trollworks.gcs.skill.Skill;
import com.trollworks.gcs.skill.SkillList;
import com.trollworks.gcs.skill.Technique;
import com.trollworks.gcs.spell.RitualMagicSpell;
import com.trollworks.gcs.spell.Spell;
import com.trollworks.gcs.spell.SpellList;
import com.trollworks.gcs.ui.RetinaIcon;
import com.trollworks.gcs.ui.image.Images;
import com.trollworks.gcs.ui.print.PrintManager;
import com.trollworks.gcs.ui.widget.outline.ListRow;
import com.trollworks.gcs.ui.widget.outline.OutlineModel;
import com.trollworks.gcs.ui.widget.outline.Row;
import com.trollworks.gcs.ui.widget.outline.RowIterator;
import com.trollworks.gcs.utility.Dice;
import com.trollworks.gcs.utility.FileType;
import com.trollworks.gcs.utility.Fixed6;
import com.trollworks.gcs.utility.I18n;
import com.trollworks.gcs.utility.text.Numbers;
import com.trollworks.gcs.utility.undo.StdUndoManager;
import com.trollworks.gcs.utility.units.LengthUnits;
import com.trollworks.gcs.utility.units.WeightUnits;
import com.trollworks.gcs.utility.units.WeightValue;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A GURPS character. */
public class GURPSCharacter extends DataFile {
    private static final int                                 CURRENT_VERSION                      = 5;
    /**
     * The version where equipment was separated out into different lists based on carried/not
     * carried status.
     */
    public static final  int                                 SEPARATED_EQUIPMENT_VERSION          = 4;
    /**
     * The version where HP and FP damage tracking was introduced, rather than a free-form text
     * field.
     */
    public static final  int                                 HP_FP_DAMAGE_TRACKING                = 5;
    private static final String                              TAG_ROOT                             = "character";
    private static final String                              TAG_CREATED_DATE                     = "created_date";
    private static final String                              TAG_MODIFIED_DATE                    = "modified_date";
    private static final String                              TAG_HP_DAMAGE                        = "hp_damage";
    private static final String                              TAG_FP_DAMAGE                        = "fp_damage";
    private static final String                              TAG_UNSPENT_POINTS                   = "unspent_points";
    private static final String                              TAG_TOTAL_POINTS                     = "total_points";
    private static final String                              TAG_INCLUDE_PUNCH                    = "include_punch";
    private static final String                              TAG_INCLUDE_KICK                     = "include_kick";
    private static final String                              TAG_INCLUDE_BOOTS                    = "include_kick_with_boots";
    /** The prefix for all character IDs. */
    public static final  String                              CHARACTER_PREFIX                     = "gcs.";
    /** The field ID for last modified date changes. */
    public static final  String                              ID_LAST_MODIFIED                     = CHARACTER_PREFIX + "LastModifiedDate";
    /** The field ID for created on date changes. */
    public static final  String                              ID_CREATED_ON                        = CHARACTER_PREFIX + "CreatedOn";
    /** The field ID for include punch changes. */
    public static final  String                              ID_INCLUDE_PUNCH                     = CHARACTER_PREFIX + "IncludePunch";
    /** The field ID for include kick changes. */
    public static final  String                              ID_INCLUDE_KICK                      = CHARACTER_PREFIX + "IncludeKickFeet";
    /** The field ID for include kick with boots changes. */
    public static final  String                              ID_INCLUDE_BOOTS                     = CHARACTER_PREFIX + "IncludeKickBoots";
    /**
     * The prefix used to indicate a point value is requested from {@link #getValueForID(String)}.
     */
    public static final  String                              POINTS_PREFIX                        = CHARACTER_PREFIX + "points.";
    /** The prefix used in front of all IDs for basic attributes. */
    public static final  String                              ATTRIBUTES_PREFIX                    = CHARACTER_PREFIX + "ba.";
    /** The field ID for strength (ST) changes. */
    public static final  String                              ID_STRENGTH                          = ATTRIBUTES_PREFIX + BonusAttributeType.ST.name();
    /** The field ID for lifting strength bonuses -- used by features. */
    public static final  String                              ID_LIFTING_STRENGTH                  = ID_STRENGTH + AttributeBonusLimitation.LIFTING_ONLY.name();
    /** The field ID for striking strength bonuses -- used by features. */
    public static final  String                              ID_STRIKING_STRENGTH                 = ID_STRENGTH + AttributeBonusLimitation.STRIKING_ONLY.name();
    /** The field ID for dexterity (DX) changes. */
    public static final  String                              ID_DEXTERITY                         = ATTRIBUTES_PREFIX + BonusAttributeType.DX.name();
    /** The field ID for intelligence (IQ) changes. */
    public static final  String                              ID_INTELLIGENCE                      = ATTRIBUTES_PREFIX + BonusAttributeType.IQ.name();
    /** The field ID for health (HT) changes. */
    public static final  String                              ID_HEALTH                            = ATTRIBUTES_PREFIX + BonusAttributeType.HT.name();
    /** The field ID for perception changes. */
    public static final  String                              ID_PERCEPTION                        = ATTRIBUTES_PREFIX + BonusAttributeType.PERCEPTION.name();
    /** The field ID for vision changes. */
    public static final  String                              ID_VISION                            = ATTRIBUTES_PREFIX + BonusAttributeType.VISION.name();
    /** The field ID for hearing changes. */
    public static final  String                              ID_HEARING                           = ATTRIBUTES_PREFIX + BonusAttributeType.HEARING.name();
    /** The field ID for taste changes. */
    public static final  String                              ID_TASTE_AND_SMELL                   = ATTRIBUTES_PREFIX + BonusAttributeType.TASTE_SMELL.name();
    /** The field ID for smell changes. */
    public static final  String                              ID_TOUCH                             = ATTRIBUTES_PREFIX + BonusAttributeType.TOUCH.name();
    /** The field ID for will changes. */
    public static final  String                              ID_WILL                              = ATTRIBUTES_PREFIX + BonusAttributeType.WILL.name();
    /** The field ID for fright check changes. */
    public static final  String                              ID_FRIGHT_CHECK                      = ATTRIBUTES_PREFIX + BonusAttributeType.FRIGHT_CHECK.name();
    /** The field ID for basic speed changes. */
    public static final  String                              ID_BASIC_SPEED                       = ATTRIBUTES_PREFIX + BonusAttributeType.SPEED.name();
    /** The field ID for basic move changes. */
    public static final  String                              ID_BASIC_MOVE                        = ATTRIBUTES_PREFIX + BonusAttributeType.MOVE.name();
    /** The prefix used in front of all IDs for dodge changes. */
    public static final  String                              DODGE_PREFIX                         = ATTRIBUTES_PREFIX + BonusAttributeType.DODGE.name() + "#.";
    /** The field ID for dodge bonus changes. */
    public static final  String                              ID_DODGE_BONUS                       = ATTRIBUTES_PREFIX + BonusAttributeType.DODGE.name();
    /** The field ID for parry bonus changes. */
    public static final  String                              ID_PARRY_BONUS                       = ATTRIBUTES_PREFIX + BonusAttributeType.PARRY.name();
    /** The field ID for block bonus changes. */
    public static final  String                              ID_BLOCK_BONUS                       = ATTRIBUTES_PREFIX + BonusAttributeType.BLOCK.name();
    /** The prefix used in front of all IDs for move changes. */
    public static final  String                              MOVE_PREFIX                          = ATTRIBUTES_PREFIX + BonusAttributeType.MOVE.name() + "#.";
    /** The field ID for carried weight changes. */
    public static final  String                              ID_CARRIED_WEIGHT                    = CHARACTER_PREFIX + "CarriedWeight";
    /** The field ID for carried wealth changes. */
    public static final  String                              ID_CARRIED_WEALTH                    = CHARACTER_PREFIX + "CarriedWealth";
    /** The field ID for other wealth changes. */
    public static final  String                              ID_NOT_CARRIED_WEALTH                = CHARACTER_PREFIX + "NotCarriedWealth";
    /** The prefix used in front of all IDs for encumbrance changes. */
    public static final  String                              MAXIMUM_CARRY_PREFIX                 = ATTRIBUTES_PREFIX + "MaximumCarry";
    private static final String                              LIFT_PREFIX                          = ATTRIBUTES_PREFIX + "lift.";
    /** The field ID for basic lift changes. */
    public static final  String                              ID_BASIC_LIFT                        = LIFT_PREFIX + "BasicLift";
    /** The field ID for one-handed lift changes. */
    public static final  String                              ID_ONE_HANDED_LIFT                   = LIFT_PREFIX + "OneHandedLift";
    /** The field ID for two-handed lift changes. */
    public static final  String                              ID_TWO_HANDED_LIFT                   = LIFT_PREFIX + "TwoHandedLift";
    /** The field ID for shove and knock over changes. */
    public static final  String                              ID_SHOVE_AND_KNOCK_OVER              = LIFT_PREFIX + "ShoveAndKnockOver";
    /** The field ID for running shove and knock over changes. */
    public static final  String                              ID_RUNNING_SHOVE_AND_KNOCK_OVER      = LIFT_PREFIX + "RunningShoveAndKnockOver";
    /** The field ID for carry on back changes. */
    public static final  String                              ID_CARRY_ON_BACK                     = LIFT_PREFIX + "CarryOnBack";
    /** The field ID for carry on back changes. */
    public static final  String                              ID_SHIFT_SLIGHTLY                    = LIFT_PREFIX + "ShiftSlightly";
    /** The prefix used in front of all IDs for point summaries. */
    public static final  String                              POINT_SUMMARY_PREFIX                 = CHARACTER_PREFIX + "ps.";
    /** The field ID for point total changes. */
    public static final  String                              ID_TOTAL_POINTS                      = POINT_SUMMARY_PREFIX + "TotalPoints";
    /** The field ID for attribute point summary changes. */
    public static final  String                              ID_ATTRIBUTE_POINTS                  = POINT_SUMMARY_PREFIX + "AttributePoints";
    /** The field ID for advantage point summary changes. */
    public static final  String                              ID_ADVANTAGE_POINTS                  = POINT_SUMMARY_PREFIX + "AdvantagePoints";
    /** The field ID for disadvantage point summary changes. */
    public static final  String                              ID_DISADVANTAGE_POINTS               = POINT_SUMMARY_PREFIX + "DisadvantagePoints";
    /** The field ID for quirk point summary changes. */
    public static final  String                              ID_QUIRK_POINTS                      = POINT_SUMMARY_PREFIX + "QuirkPoints";
    /** The field ID for skill point summary changes. */
    public static final  String                              ID_SKILL_POINTS                      = POINT_SUMMARY_PREFIX + "SkillPoints";
    /** The field ID for spell point summary changes. */
    public static final  String                              ID_SPELL_POINTS                      = POINT_SUMMARY_PREFIX + "SpellPoints";
    /** The field ID for racial point summary changes. */
    public static final  String                              ID_RACE_POINTS                       = POINT_SUMMARY_PREFIX + "RacePoints";
    /** The field ID for unspent point changes. */
    public static final  String                              ID_UNSPENT_POINTS                    = POINT_SUMMARY_PREFIX + "UnspentPoints";
    /** The prefix used in front of all IDs for basic damage. */
    public static final  String                              BASIC_DAMAGE_PREFIX                  = CHARACTER_PREFIX + "bd.";
    /** The field ID for basic thrust damage changes. */
    public static final  String                              ID_BASIC_THRUST                      = BASIC_DAMAGE_PREFIX + "Thrust";
    /** The field ID for basic swing damage changes. */
    public static final  String                              ID_BASIC_SWING                       = BASIC_DAMAGE_PREFIX + "Swing";
    private static final String                              HIT_POINTS_PREFIX                    = ATTRIBUTES_PREFIX + "derived_hp.";
    /** The field ID for hit point changes. */
    public static final  String                              ID_HIT_POINTS                        = ATTRIBUTES_PREFIX + BonusAttributeType.HP.name();
    /** The field ID for hit point damage changes. */
    public static final  String                              ID_HIT_POINTS_DAMAGE                 = HIT_POINTS_PREFIX + "Damage";
    /** The field ID for current hit point changes. */
    public static final  String                              ID_CURRENT_HP                        = HIT_POINTS_PREFIX + "Current";
    /** The field ID for reeling hit point changes. */
    public static final  String                              ID_REELING_HIT_POINTS                = HIT_POINTS_PREFIX + "Reeling";
    /** The field ID for unconscious check hit point changes. */
    public static final  String                              ID_UNCONSCIOUS_CHECKS_HIT_POINTS     = HIT_POINTS_PREFIX + "UnconsciousChecks";
    /** The field ID for death check #1 hit point changes. */
    public static final  String                              ID_DEATH_CHECK_1_HIT_POINTS          = HIT_POINTS_PREFIX + "DeathCheck1";
    /** The field ID for death check #2 hit point changes. */
    public static final  String                              ID_DEATH_CHECK_2_HIT_POINTS          = HIT_POINTS_PREFIX + "DeathCheck2";
    /** The field ID for death check #3 hit point changes. */
    public static final  String                              ID_DEATH_CHECK_3_HIT_POINTS          = HIT_POINTS_PREFIX + "DeathCheck3";
    /** The field ID for death check #4 hit point changes. */
    public static final  String                              ID_DEATH_CHECK_4_HIT_POINTS          = HIT_POINTS_PREFIX + "DeathCheck4";
    /** The field ID for dead hit point changes. */
    public static final  String                              ID_DEAD_HIT_POINTS                   = HIT_POINTS_PREFIX + "Dead";
    private static final String                              FATIGUE_POINTS_PREFIX                = ATTRIBUTES_PREFIX + "derived_fp.";
    /** The field ID for fatigue point changes. */
    public static final  String                              ID_FATIGUE_POINTS                    = ATTRIBUTES_PREFIX + BonusAttributeType.FP.name();
    /** The field ID for fatigue point damage changes. */
    public static final  String                              ID_FATIGUE_POINTS_DAMAGE             = FATIGUE_POINTS_PREFIX + "Damage";
    /** The field ID for current fatigue point changes. */
    public static final  String                              ID_CURRENT_FP                        = FATIGUE_POINTS_PREFIX + "Current";
    /** The field ID for tired fatigue point changes. */
    public static final  String                              ID_TIRED_FATIGUE_POINTS              = FATIGUE_POINTS_PREFIX + "Tired";
    /** The field ID for unconscious check fatigue point changes. */
    public static final  String                              ID_UNCONSCIOUS_CHECKS_FATIGUE_POINTS = FATIGUE_POINTS_PREFIX + "UnconsciousChecks";
    /** The field ID for unconscious fatigue point changes. */
    public static final  String                              ID_UNCONSCIOUS_FATIGUE_POINTS        = FATIGUE_POINTS_PREFIX + "Unconscious";
    private              long                                mLastModified;
    private              long                                mCreatedOn;
    private              HashMap<String, ArrayList<Feature>> mFeatureMap;
    private              int                                 mStrength;
    private              int                                 mStrengthBonus;
    private              int                                 mLiftingStrengthBonus;
    private              int                                 mStrikingStrengthBonus;
    private              int                                 mStrengthCostReduction;
    private              int                                 mDexterity;
    private              int                                 mDexterityBonus;
    private              int                                 mDexterityCostReduction;
    private              int                                 mIntelligence;
    private              int                                 mIntelligenceBonus;
    private              int                                 mIntelligenceCostReduction;
    private              int                                 mHealth;
    private              int                                 mHealthBonus;
    private              int                                 mHealthCostReduction;
    private              int                                 mWill;
    private              int                                 mWillBonus;
    private              int                                 mFrightCheckBonus;
    private              int                                 mPerception;
    private              int                                 mPerceptionBonus;
    private              int                                 mVisionBonus;
    private              int                                 mHearingBonus;
    private              int                                 mTasteAndSmellBonus;
    private              int                                 mTouchBonus;
    private              int                                 mHitPointsDamage;
    private              int                                 mHitPoints;
    private              int                                 mHitPointBonus;
    private              int                                 mFatiguePoints;
    private              int                                 mFatiguePointsDamage;
    private              int                                 mFatiguePointBonus;
    private              double                              mSpeed;
    private              double                              mSpeedBonus;
    private              int                                 mMove;
    private              int                                 mMoveBonus;
    private              int                                 mDodgeBonus;
    private              int                                 mParryBonus;
    private              int                                 mBlockBonus;
    private              int                                 mTotalPoints;
    private              Profile                             mDescription;
    private              Armor                               mArmor;
    private              OutlineModel                        mAdvantages;
    private              OutlineModel                        mSkills;
    private              OutlineModel                        mSpells;
    private              OutlineModel                        mEquipment;
    private              OutlineModel                        mOtherEquipment;
    private              OutlineModel                        mNotes;
    private              boolean                             mDidModify;
    private              boolean                             mNeedAttributePointCalculation;
    private              boolean                             mNeedAdvantagesPointCalculation;
    private              boolean                             mNeedSkillPointCalculation;
    private              boolean                             mNeedSpellPointCalculation;
    private              boolean                             mNeedEquipmentCalculation;
    private              WeightValue                         mCachedWeightCarried;
    private              Fixed6                              mCachedWealthCarried;
    private              Fixed6                              mCachedWealthNotCarried;
    private              int                                 mCachedAttributePoints;
    private              int                                 mCachedAdvantagePoints;
    private              int                                 mCachedDisadvantagePoints;
    private              int                                 mCachedQuirkPoints;
    private              int                                 mCachedSkillPoints;
    private              int                                 mCachedSpellPoints;
    private              int                                 mCachedRacePoints;
    private              boolean                             mSkillsUpdated;
    private              boolean                             mSpellsUpdated;
    private              PrintManager                        mPageSettings;
    private              boolean                             mIncludePunch;
    private              boolean                             mIncludeKick;
    private              boolean                             mIncludeKickBoots;

    /** Creates a new character with only default values set. */
    public GURPSCharacter() {
        characterInitialize(true);
        calculateAll();
    }

    /**
     * Creates a new character from the specified file.
     *
     * @param file The file to load the data from.
     * @throws IOException if the data cannot be read or the file doesn't contain a valid character
     *                     sheet.
     */
    public GURPSCharacter(File file) throws IOException {
        load(file);
    }

    private void characterInitialize(boolean full) {
        mFeatureMap = new HashMap<>();
        mAdvantages = new OutlineModel();
        mSkills = new OutlineModel();
        mSpells = new OutlineModel();
        mEquipment = new OutlineModel();
        mOtherEquipment = new OutlineModel();
        mOtherEquipment.setProperty(EquipmentList.TAG_OTHER_ROOT, Boolean.TRUE);
        mNotes = new OutlineModel();
        mTotalPoints = SheetPreferences.getInitialPoints();
        mStrength = 10;
        mDexterity = 10;
        mIntelligence = 10;
        mHealth = 10;
        mHitPointsDamage = 0;
        mFatiguePointsDamage = 0;
        mDescription = new Profile(this, full);
        mArmor = new Armor(this);
        mIncludePunch = true;
        mIncludeKick = true;
        mIncludeKickBoots = true;
        mCachedWeightCarried = new WeightValue(Fixed6.ZERO, DisplayPreferences.getWeightUnits());
        mPageSettings = OutputPreferences.getDefaultPageSettings();
        mLastModified = System.currentTimeMillis();
        mCreatedOn = mLastModified;
        // This will force the long value to match the string value.
        setCreatedOn(getCreatedOn());
    }

    /** @return The page settings. May return {@code null} if no printer has been defined. */
    public PrintManager getPageSettings() {
        return mPageSettings;
    }

    @Override
    public FileType getFileType() {
        return FileType.SHEET;
    }

    @Override
    public RetinaIcon getFileIcons() {
        return Images.GCS_FILE;
    }

    @Override
    protected final void loadSelf(XMLReader reader, LoadState state) throws IOException {
        String marker        = reader.getMarker();
        int    unspentPoints = 0;
        int    currentHP     = Integer.MIN_VALUE;
        int    currentFP     = Integer.MIN_VALUE;
        characterInitialize(false);
        do {
            if (reader.next() == XMLNodeType.START_TAG) {
                String name = reader.getName();

                if (state.mDataFileVersion == 0) {
                    if (mDescription.loadTag(reader, name)) {
                        continue;
                    }
                }

                if (state.mDataFileVersion < HP_FP_DAMAGE_TRACKING) {
                    if ("current_hp".equals(name)) {
                        currentHP = reader.readInteger(Integer.MIN_VALUE);
                        continue;
                    } else if ("current_fp".equals(name)) {
                        currentFP = reader.readInteger(Integer.MIN_VALUE);
                        continue;
                    }
                }

                if (Profile.TAG_ROOT.equals(name)) {
                    mDescription.load(reader);
                } else if (TAG_CREATED_DATE.equals(name)) {
                    mCreatedOn = Numbers.extractDate(reader.readText());
                } else if (TAG_MODIFIED_DATE.equals(name)) {
                    mLastModified = Numbers.extractDateTime(reader.readText());
                } else if (BonusAttributeType.HP.getXMLTag().equals(name)) {
                    mHitPoints = reader.readInteger(0);
                } else if (TAG_HP_DAMAGE.equals(name)) {
                    mHitPointsDamage = reader.readInteger(0);
                } else if (BonusAttributeType.FP.getXMLTag().equals(name)) {
                    mFatiguePoints = reader.readInteger(0);
                } else if (TAG_FP_DAMAGE.equals(name)) {
                    mFatiguePointsDamage = reader.readInteger(0);
                } else if (TAG_UNSPENT_POINTS.equals(name)) {
                    unspentPoints = reader.readInteger(0);
                } else if (TAG_TOTAL_POINTS.equals(name)) {
                    mTotalPoints = reader.readInteger(0);
                } else if (BonusAttributeType.ST.getXMLTag().equals(name)) {
                    mStrength = reader.readInteger(0);
                } else if (BonusAttributeType.DX.getXMLTag().equals(name)) {
                    mDexterity = reader.readInteger(0);
                } else if (BonusAttributeType.IQ.getXMLTag().equals(name)) {
                    mIntelligence = reader.readInteger(0);
                } else if (BonusAttributeType.HT.getXMLTag().equals(name)) {
                    mHealth = reader.readInteger(0);
                } else if (BonusAttributeType.WILL.getXMLTag().equals(name)) {
                    mWill = reader.readInteger(0);
                } else if (BonusAttributeType.PERCEPTION.getXMLTag().equals(name)) {
                    mPerception = reader.readInteger(0);
                } else if (BonusAttributeType.SPEED.getXMLTag().equals(name)) {
                    mSpeed = reader.readDouble(0.0);
                } else if (BonusAttributeType.MOVE.getXMLTag().equals(name)) {
                    mMove = reader.readInteger(0);
                } else if (TAG_INCLUDE_PUNCH.equals(name)) {
                    mIncludePunch = reader.readBoolean();
                } else if (TAG_INCLUDE_KICK.equals(name)) {
                    mIncludeKick = reader.readBoolean();
                } else if (TAG_INCLUDE_BOOTS.equals(name)) {
                    mIncludeKickBoots = reader.readBoolean();
                } else if (AdvantageList.TAG_ROOT.equals(name)) {
                    loadAdvantageList(reader, state);
                } else if (SkillList.TAG_ROOT.equals(name)) {
                    loadSkillList(reader, state);
                } else if (SpellList.TAG_ROOT.equals(name)) {
                    loadSpellList(reader, state);
                } else if (EquipmentList.TAG_CARRIED_ROOT.equals(name)) {
                    loadEquipmentList(reader, state, mEquipment);
                } else if (EquipmentList.TAG_OTHER_ROOT.equals(name)) {
                    loadEquipmentList(reader, state, mOtherEquipment);
                } else if (NoteList.TAG_ROOT.equals(name)) {
                    loadNoteList(reader, state);
                } else if (mPageSettings != null && PrintManager.TAG_ROOT.equals(name)) {
                    mPageSettings.load(reader);
                } else {
                    reader.skipTag(name);
                }
            }
        } while (reader.withinMarker(marker));

        // Loop through the skills and update their levels. It is necessary to do this here and not
        // as they are loaded, since references to defaults won't work until the entire list is
        // available.
        for (Skill skill : getSkillsIterator()) {
            skill.updateLevel(false);
        }

        calculateAll();
        if (unspentPoints != 0) {
            setUnspentPoints(unspentPoints);
        }

        if (state.mDataFileVersion < HP_FP_DAMAGE_TRACKING) {
            if (currentHP != Integer.MIN_VALUE) {
                mHitPointsDamage = -Math.min(currentHP - getHitPoints(), 0);
            }
            if (currentFP != Integer.MIN_VALUE) {
                mFatiguePointsDamage = -Math.min(currentFP - getFatiguePoints(), 0);
            }
        }
    }

    private void loadAdvantageList(XMLReader reader, LoadState state) throws IOException {
        String marker = reader.getMarker();
        do {
            if (reader.next() == XMLNodeType.START_TAG) {
                String name = reader.getName();
                if (Advantage.TAG_ADVANTAGE.equals(name) || Advantage.TAG_ADVANTAGE_CONTAINER.equals(name)) {
                    mAdvantages.addRow(new Advantage(this, reader, state), true);
                } else {
                    reader.skipTag(name);
                }
            }
        } while (reader.withinMarker(marker));
    }

    private void loadSkillList(XMLReader reader, LoadState state) throws IOException {
        String marker = reader.getMarker();
        do {
            if (reader.next() == XMLNodeType.START_TAG) {
                String name = reader.getName();
                if (Skill.TAG_SKILL.equals(name) || Skill.TAG_SKILL_CONTAINER.equals(name)) {
                    mSkills.addRow(new Skill(this, reader, state), true);
                } else if (Technique.TAG_TECHNIQUE.equals(name)) {
                    mSkills.addRow(new Technique(this, reader, state), true);
                } else {
                    reader.skipTag(name);
                }
            }
        } while (reader.withinMarker(marker));
    }

    private void loadSpellList(XMLReader reader, LoadState state) throws IOException {
        String marker = reader.getMarker();
        do {
            if (reader.next() == XMLNodeType.START_TAG) {
                String name = reader.getName();
                if (Spell.TAG_SPELL.equals(name) || Spell.TAG_SPELL_CONTAINER.equals(name)) {
                    mSpells.addRow(new Spell(this, reader, state), true);
                } else if (RitualMagicSpell.TAG_RITUAL_MAGIC_SPELL.equals(name)) {
                    mSpells.addRow(new RitualMagicSpell(this, reader, state), true);
                } else {
                    reader.skipTag(name);
                }
            }
        } while (reader.withinMarker(marker));
    }

    private void loadEquipmentList(XMLReader reader, LoadState state, OutlineModel equipmentList) throws IOException {
        String marker = reader.getMarker();
        do {
            if (reader.next() == XMLNodeType.START_TAG) {
                String name = reader.getName();
                if (Equipment.TAG_EQUIPMENT.equals(name) || Equipment.TAG_EQUIPMENT_CONTAINER.equals(name)) {
                    state.mUncarriedEquipment = new HashSet<>();
                    Equipment equipment = new Equipment(this, reader, state);
                    if (state.mDataFileVersion < SEPARATED_EQUIPMENT_VERSION && equipmentList == mEquipment && !state.mUncarriedEquipment.isEmpty()) {
                        if (addToEquipment(state.mUncarriedEquipment, equipment)) {
                            equipmentList.addRow(equipment, true);
                        }
                    } else {
                        equipmentList.addRow(equipment, true);
                    }
                } else {
                    reader.skipTag(name);
                }
            }
        } while (reader.withinMarker(marker));
    }

    private boolean addToEquipment(HashSet<Equipment> uncarried, Equipment equipment) {
        if (uncarried.contains(equipment)) {
            mOtherEquipment.addRow(equipment, true);
            return false;
        }
        List<Row> children = equipment.getChildren();
        if (children != null) {
            for (Row child : new ArrayList<>(children)) {
                if (!addToEquipment(uncarried, (Equipment) child)) {
                    equipment.removeChild(child);
                }
            }
        }
        return true;
    }

    private void loadNoteList(XMLReader reader, LoadState state) throws IOException {
        String marker = reader.getMarker();
        do {
            if (reader.next() == XMLNodeType.START_TAG) {
                String name = reader.getName();
                if (Note.TAG_NOTE.equals(name) || Note.TAG_NOTE_CONTAINER.equals(name)) {
                    mNotes.addRow(new Note(this, reader, state), true);
                } else {
                    reader.skipTag(name);
                }
            }
        } while (reader.withinMarker(marker));
    }

    private void calculateAll() {
        calculateAttributePoints();
        calculateAdvantagePoints();
        calculateSkillPoints();
        calculateSpellPoints();
        calculateWeightAndWealthCarried(false);
        calculateWealthNotCarried(false);
    }

    @Override
    public int getXMLTagVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public String getXMLTagName() {
        return TAG_ROOT;
    }

    @Override
    protected void saveSelf(XMLWriter out) {
        out.simpleTag(TAG_CREATED_DATE, DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(mCreatedOn)));
        out.simpleTag(TAG_MODIFIED_DATE, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(mLastModified)));
        mDescription.save(out);
        out.simpleTag(BonusAttributeType.HP.getXMLTag(), mHitPoints);
        out.simpleTagNotZero(TAG_HP_DAMAGE, mHitPointsDamage);
        out.simpleTag(BonusAttributeType.FP.getXMLTag(), mFatiguePoints);
        out.simpleTagNotZero(TAG_FP_DAMAGE, mFatiguePointsDamage);
        out.simpleTag(TAG_TOTAL_POINTS, mTotalPoints);
        out.simpleTag(BonusAttributeType.ST.getXMLTag(), mStrength);
        out.simpleTag(BonusAttributeType.DX.getXMLTag(), mDexterity);
        out.simpleTag(BonusAttributeType.IQ.getXMLTag(), mIntelligence);
        out.simpleTag(BonusAttributeType.HT.getXMLTag(), mHealth);
        out.simpleTag(BonusAttributeType.WILL.getXMLTag(), mWill);
        out.simpleTag(BonusAttributeType.PERCEPTION.getXMLTag(), mPerception);
        out.simpleTag(BonusAttributeType.SPEED.getXMLTag(), mSpeed);
        out.simpleTag(BonusAttributeType.MOVE.getXMLTag(), mMove);
        out.simpleTag(TAG_INCLUDE_PUNCH, mIncludePunch);
        out.simpleTag(TAG_INCLUDE_KICK, mIncludeKick);
        out.simpleTag(TAG_INCLUDE_BOOTS, mIncludeKickBoots);
        saveList(AdvantageList.TAG_ROOT, mAdvantages, out);
        saveList(SkillList.TAG_ROOT, mSkills, out);
        saveList(SpellList.TAG_ROOT, mSpells, out);
        saveList(EquipmentList.TAG_CARRIED_ROOT, mEquipment, out);
        saveList(EquipmentList.TAG_OTHER_ROOT, mOtherEquipment, out);
        saveList(NoteList.TAG_ROOT, mNotes, out);
        if (mPageSettings != null) {
            mPageSettings.save(out, LengthUnits.IN);
        }
    }

    private static void saveList(String tag, OutlineModel model, XMLWriter out) {
        if (model.getRowCount() > 0) {
            out.startSimpleTagEOL(tag);
            for (ListRow row : new FilteredIterator<>(model.getTopLevelRows(), ListRow.class)) {
                row.save(out, false);
            }
            out.endTagEOL(tag, true);
        }
    }

    /**
     * @param id The field ID to retrieve the data for.
     * @return The value of the specified field ID, or {@code null} if the field ID is invalid.
     */
    public Object getValueForID(String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith(POINTS_PREFIX)) {
            id = id.substring(POINTS_PREFIX.length());
            if (ID_STRENGTH.equals(id)) {
                return Integer.valueOf(getStrengthPoints());
            } else if (ID_DEXTERITY.equals(id)) {
                return Integer.valueOf(getDexterityPoints());
            } else if (ID_INTELLIGENCE.equals(id)) {
                return Integer.valueOf(getIntelligencePoints());
            } else if (ID_HEALTH.equals(id)) {
                return Integer.valueOf(getHealthPoints());
            } else if (ID_WILL.equals(id)) {
                return Integer.valueOf(getWillPoints());
            } else if (ID_PERCEPTION.equals(id)) {
                return Integer.valueOf(getPerceptionPoints());
            } else if (ID_BASIC_SPEED.equals(id)) {
                return Integer.valueOf(getBasicSpeedPoints());
            } else if (ID_BASIC_MOVE.equals(id)) {
                return Integer.valueOf(getBasicMovePoints());
            } else if (ID_FATIGUE_POINTS.equals(id)) {
                return Integer.valueOf(getFatiguePointPoints());
            } else if (ID_HIT_POINTS.equals(id)) {
                return Integer.valueOf(getHitPointPoints());
            }
            return null;
        } else if (ID_LAST_MODIFIED.equals(id)) {
            return getLastModified();
        } else if (ID_CREATED_ON.equals(id)) {
            return Long.valueOf(getCreatedOn());
        } else if (ID_STRENGTH.equals(id)) {
            return Integer.valueOf(getStrength());
        } else if (ID_DEXTERITY.equals(id)) {
            return Integer.valueOf(getDexterity());
        } else if (ID_INTELLIGENCE.equals(id)) {
            return Integer.valueOf(getIntelligence());
        } else if (ID_HEALTH.equals(id)) {
            return Integer.valueOf(getHealth());
        } else if (ID_BASIC_SPEED.equals(id)) {
            return Double.valueOf(getBasicSpeed());
        } else if (ID_BASIC_MOVE.equals(id)) {
            return Integer.valueOf(getBasicMove());
        } else if (ID_BASIC_LIFT.equals(id)) {
            return getBasicLift();
        } else if (ID_PERCEPTION.equals(id)) {
            return Integer.valueOf(getPerception());
        } else if (ID_VISION.equals(id)) {
            return Integer.valueOf(getVision());
        } else if (ID_HEARING.equals(id)) {
            return Integer.valueOf(getHearing());
        } else if (ID_TASTE_AND_SMELL.equals(id)) {
            return Integer.valueOf(getTasteAndSmell());
        } else if (ID_TOUCH.equals(id)) {
            return Integer.valueOf(getTouch());
        } else if (ID_WILL.equals(id)) {
            return Integer.valueOf(getWill());
        } else if (ID_FRIGHT_CHECK.equals(id)) {
            return Integer.valueOf(getFrightCheck());
        } else if (ID_ATTRIBUTE_POINTS.equals(id)) {
            return Integer.valueOf(getAttributePoints());
        } else if (ID_ADVANTAGE_POINTS.equals(id)) {
            return Integer.valueOf(getAdvantagePoints());
        } else if (ID_DISADVANTAGE_POINTS.equals(id)) {
            return Integer.valueOf(getDisadvantagePoints());
        } else if (ID_QUIRK_POINTS.equals(id)) {
            return Integer.valueOf(getQuirkPoints());
        } else if (ID_SKILL_POINTS.equals(id)) {
            return Integer.valueOf(getSkillPoints());
        } else if (ID_SPELL_POINTS.equals(id)) {
            return Integer.valueOf(getSpellPoints());
        } else if (ID_RACE_POINTS.equals(id)) {
            return Integer.valueOf(getRacePoints());
        } else if (ID_UNSPENT_POINTS.equals(id)) {
            return Integer.valueOf(getUnspentPoints());
        } else if (ID_ONE_HANDED_LIFT.equals(id)) {
            return getOneHandedLift();
        } else if (ID_TWO_HANDED_LIFT.equals(id)) {
            return getTwoHandedLift();
        } else if (ID_SHOVE_AND_KNOCK_OVER.equals(id)) {
            return getShoveAndKnockOver();
        } else if (ID_RUNNING_SHOVE_AND_KNOCK_OVER.equals(id)) {
            return getRunningShoveAndKnockOver();
        } else if (ID_CARRY_ON_BACK.equals(id)) {
            return getCarryOnBack();
        } else if (ID_SHIFT_SLIGHTLY.equals(id)) {
            return getShiftSlightly();
        } else if (ID_TOTAL_POINTS.equals(id)) {
            return Integer.valueOf(getTotalPoints());
        } else if (ID_BASIC_THRUST.equals(id)) {
            return getThrust();
        } else if (ID_BASIC_SWING.equals(id)) {
            return getSwing();
        } else if (ID_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getHitPoints());
        } else if (ID_HIT_POINTS_DAMAGE.equals(id)) {
            return Integer.valueOf(getHitPointsDamage());
        } else if (ID_CURRENT_HP.equals(id)) {
            return Integer.valueOf(getCurrentHitPoints());
        } else if (ID_REELING_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getReelingHitPoints());
        } else if (ID_UNCONSCIOUS_CHECKS_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getUnconsciousChecksHitPoints());
        } else if (ID_DEATH_CHECK_1_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getDeathCheck1HitPoints());
        } else if (ID_DEATH_CHECK_2_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getDeathCheck2HitPoints());
        } else if (ID_DEATH_CHECK_3_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getDeathCheck3HitPoints());
        } else if (ID_DEATH_CHECK_4_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getDeathCheck4HitPoints());
        } else if (ID_DEAD_HIT_POINTS.equals(id)) {
            return Integer.valueOf(getDeadHitPoints());
        } else if (ID_FATIGUE_POINTS.equals(id)) {
            return Integer.valueOf(getFatiguePoints());
        } else if (ID_FATIGUE_POINTS_DAMAGE.equals(id)) {
            return Integer.valueOf(getFatiguePointsDamage());
        } else if (ID_CURRENT_FP.equals(id)) {
            return Integer.valueOf(getCurrentFatiguePoints());
        } else if (ID_TIRED_FATIGUE_POINTS.equals(id)) {
            return Integer.valueOf(getTiredFatiguePoints());
        } else if (ID_UNCONSCIOUS_CHECKS_FATIGUE_POINTS.equals(id)) {
            return Integer.valueOf(getUnconsciousChecksFatiguePoints());
        } else if (ID_UNCONSCIOUS_FATIGUE_POINTS.equals(id)) {
            return Integer.valueOf(getUnconsciousFatiguePoints());
        } else if (ID_PARRY_BONUS.equals(id)) {
            return Integer.valueOf(getParryBonus());
        } else if (ID_BLOCK_BONUS.equals(id)) {
            return Integer.valueOf(getBlockBonus());
        } else if (ID_DODGE_BONUS.equals(id)) {
            return Integer.valueOf(getDodgeBonus());
        } else if (id.startsWith(Profile.PROFILE_PREFIX)) {
            return mDescription.getValueForID(id);
        } else if (id.startsWith(Armor.DR_PREFIX)) {
            return mArmor.getValueForID(id);
        } else {
            for (Encumbrance encumbrance : Encumbrance.values()) {
                int index = encumbrance.ordinal();
                if ((DODGE_PREFIX + index).equals(id)) {
                    return Integer.valueOf(getDodge(encumbrance));
                }
                if ((MOVE_PREFIX + index).equals(id)) {
                    return Integer.valueOf(getMove(encumbrance));
                }
                if ((MAXIMUM_CARRY_PREFIX + index).equals(id)) {
                    return getMaximumCarry(encumbrance);
                }
            }
            return null;
        }
    }

    /**
     * @param id    The field ID to set the value for.
     * @param value The value to set.
     */
    public void setValueForID(String id, Object value) {
        if (id != null) {
            if (ID_CREATED_ON.equals(id)) {
                if (value instanceof Long) {
                    setCreatedOn(((Long) value).longValue());
                } else {
                    setCreatedOn((String) value);
                }
            } else if (ID_INCLUDE_PUNCH.equals(id)) {
                setIncludePunch(((Boolean) value).booleanValue());
            } else if (ID_INCLUDE_KICK.equals(id)) {
                setIncludeKick(((Boolean) value).booleanValue());
            } else if (ID_INCLUDE_BOOTS.equals(id)) {
                setIncludeKickBoots(((Boolean) value).booleanValue());
            } else if (ID_STRENGTH.equals(id)) {
                setStrength(((Integer) value).intValue());
            } else if (ID_DEXTERITY.equals(id)) {
                setDexterity(((Integer) value).intValue());
            } else if (ID_INTELLIGENCE.equals(id)) {
                setIntelligence(((Integer) value).intValue());
            } else if (ID_HEALTH.equals(id)) {
                setHealth(((Integer) value).intValue());
            } else if (ID_BASIC_SPEED.equals(id)) {
                setBasicSpeed(((Double) value).doubleValue());
            } else if (ID_BASIC_MOVE.equals(id)) {
                setBasicMove(((Integer) value).intValue());
            } else if (ID_PERCEPTION.equals(id)) {
                setPerception(((Integer) value).intValue());
            } else if (ID_WILL.equals(id)) {
                setWill(((Integer) value).intValue());
            } else if (ID_UNSPENT_POINTS.equals(id)) {
                setUnspentPoints(((Integer) value).intValue());
            } else if (ID_HIT_POINTS.equals(id)) {
                setHitPoints(((Integer) value).intValue());
            } else if (ID_HIT_POINTS_DAMAGE.equals(id)) {
                setHitPointsDamage(((Integer) value).intValue());
            } else if (ID_CURRENT_HP.equals(id)) {
                setHitPointsDamage(-Math.min(((Integer) value).intValue() - getHitPoints(), 0));
            } else if (ID_FATIGUE_POINTS.equals(id)) {
                setFatiguePoints(((Integer) value).intValue());
            } else if (ID_FATIGUE_POINTS_DAMAGE.equals(id)) {
                setFatiguePointsDamage(((Integer) value).intValue());
            } else if (ID_CURRENT_FP.equals(id)) {
                setFatiguePointsDamage(-Math.min(((Integer) value).intValue() - getFatiguePoints(), 0));
            } else if (id.startsWith(Profile.PROFILE_PREFIX)) {
                mDescription.setValueForID(id, value);
            } else if (id.startsWith(Armor.DR_PREFIX)) {
                mArmor.setValueForID(id, value);
            } else {
                Log.error(String.format(I18n.Text("Unable to set a value for %s"), id));
            }
        }
    }

    @Override
    protected void startNotifyAtBatchLevelZero() {
        mDidModify = false;
        mNeedAttributePointCalculation = false;
        mNeedAdvantagesPointCalculation = false;
        mNeedSkillPointCalculation = false;
        mNeedSpellPointCalculation = false;
        mNeedEquipmentCalculation = false;
    }

    @Override
    public void notify(String type, Object data) {
        super.notify(type, data);
        if (Advantage.ID_POINTS.equals(type) || Advantage.ID_ROUND_COST_DOWN.equals(type) || Advantage.ID_LEVELS.equals(type) || Advantage.ID_CONTAINER_TYPE.equals(type) || Advantage.ID_LIST_CHANGED.equals(type) || Advantage.ID_CR.equals(type) || AdvantageModifier.ID_LIST_CHANGED.equals(type) || AdvantageModifier.ID_ENABLED.equals(type)) {
            mNeedAdvantagesPointCalculation = true;
        }
        if (Skill.ID_POINTS.equals(type) || Skill.ID_LIST_CHANGED.equals(type)) {
            mNeedSkillPointCalculation = true;
        }
        if (Spell.ID_POINTS.equals(type) || Spell.ID_LIST_CHANGED.equals(type)) {
            mNeedSpellPointCalculation = true;
        }
        if (Equipment.ID_QUANTITY.equals(type) || Equipment.ID_WEIGHT.equals(type) || Equipment.ID_EXTENDED_WEIGHT.equals(type) || Equipment.ID_LIST_CHANGED.equals(type) || EquipmentModifier.ID_WEIGHT_ADJ.equals(type) || EquipmentModifier.ID_COST_ADJ.equals(type) || EquipmentModifier.ID_ENABLED.equals(type)) {
            mNeedEquipmentCalculation = true;
        }
        if (Profile.ID_SIZE_MODIFIER.equals(type) || SheetPreferences.OPTIONAL_STRENGTH_RULES_PREF_KEY.equals(type)) {
            mNeedAttributePointCalculation = true;
        }
    }

    @Override
    protected void notifyOccured() {
        mDidModify = true;
    }

    @Override
    protected void endNotifyAtBatchLevelOne() {
        if (mNeedAttributePointCalculation) {
            calculateAttributePoints();
            notify(ID_ATTRIBUTE_POINTS, Integer.valueOf(getAttributePoints()));
        }
        if (mNeedAdvantagesPointCalculation) {
            calculateAdvantagePoints();
            notify(ID_ADVANTAGE_POINTS, Integer.valueOf(getAdvantagePoints()));
            notify(ID_DISADVANTAGE_POINTS, Integer.valueOf(getDisadvantagePoints()));
            notify(ID_QUIRK_POINTS, Integer.valueOf(getQuirkPoints()));
            notify(ID_RACE_POINTS, Integer.valueOf(getRacePoints()));
        }
        if (mNeedSkillPointCalculation) {
            calculateSkillPoints();
            notify(ID_SKILL_POINTS, Integer.valueOf(getSkillPoints()));
        }
        if (mNeedSpellPointCalculation) {
            calculateSpellPoints();
            notify(ID_SPELL_POINTS, Integer.valueOf(getSpellPoints()));
        }
        if (mNeedAttributePointCalculation || mNeedAdvantagesPointCalculation || mNeedSkillPointCalculation || mNeedSpellPointCalculation) {
            notify(ID_UNSPENT_POINTS, Integer.valueOf(getUnspentPoints()));
        }
        if (mNeedEquipmentCalculation) {
            calculateWeightAndWealthCarried(true);
            calculateWealthNotCarried(true);
        }
        if (mDidModify) {
            long now = System.currentTimeMillis();
            if (mLastModified != now) {
                mLastModified = now;
                notify(ID_LAST_MODIFIED, Long.valueOf(mLastModified));
            }
        }
    }

    /** @return The last modified date and time. */
    public String getLastModified() {
        Date date = new Date(mLastModified);
        return MessageFormat.format(I18n.Text("Modified at {0} on {1}"), DateFormat.getTimeInstance(DateFormat.SHORT).format(date), DateFormat.getDateInstance(DateFormat.MEDIUM).format(date));
    }

    /** @return The created on date. */
    public long getCreatedOn() {
        return mCreatedOn;
    }

    /**
     * Sets the created on date.
     *
     * @param date The new created on date.
     */
    public void setCreatedOn(long date) {
        if (mCreatedOn != date) {
            Long value = Long.valueOf(date);
            postUndoEdit(I18n.Text("Created On Change"), ID_CREATED_ON, Long.valueOf(mCreatedOn), value);
            mCreatedOn = date;
            notifySingle(ID_CREATED_ON, value);
        }
    }

    /**
     * Sets the created on date.
     *
     * @param date The new created on date.
     */
    public void setCreatedOn(String date) {
        setCreatedOn(Numbers.extractDate(date));
    }

    private void updateSkills() {
        for (Skill skill : getSkillsIterator()) {
            skill.updateLevel(true);
        }
        mSkillsUpdated = true;
    }

    private void updateSpells() {
        for (Spell spell : getSpellsIterator()) {
            spell.updateLevel(true);
        }
        mSpellsUpdated = true;
    }

    /** @return The strength (ST). */
    public int getStrength() {
        return mStrength + mStrengthBonus;
    }

    /**
     * Sets the strength (ST).
     *
     * @param strength The new strength.
     */
    public void setStrength(int strength) {
        int oldStrength = getStrength();
        if (oldStrength != strength) {
            postUndoEdit(I18n.Text("Strength Change"), ID_STRENGTH, Integer.valueOf(oldStrength), Integer.valueOf(strength));
            updateStrengthInfo(strength - mStrengthBonus, mStrengthBonus, mLiftingStrengthBonus, mStrikingStrengthBonus);
        }
    }

    /** @return The current strength bonus from features. */
    public int getStrengthBonus() {
        return mStrengthBonus;
    }

    /** @param bonus The new strength bonus. */
    public void setStrengthBonus(int bonus) {
        if (mStrengthBonus != bonus) {
            updateStrengthInfo(mStrength, bonus, mLiftingStrengthBonus, mStrikingStrengthBonus);
        }
    }

    /** @param reduction The cost reduction for strength. */
    public void setStrengthCostReduction(int reduction) {
        if (mStrengthCostReduction != reduction) {
            mStrengthCostReduction = reduction;
            mNeedAttributePointCalculation = true;
        }
    }

    /** @return The current lifting strength bonus from features. */
    public int getLiftingStrengthBonus() {
        return mLiftingStrengthBonus;
    }

    /** @param bonus The new lifting strength bonus. */
    public void setLiftingStrengthBonus(int bonus) {
        if (mLiftingStrengthBonus != bonus) {
            updateStrengthInfo(mStrength, mStrengthBonus, bonus, mStrikingStrengthBonus);
        }
    }

    /** @return The current striking strength bonus from features. */
    public int getStrikingStrengthBonus() {
        return mStrikingStrengthBonus;
    }

    /** @param bonus The new striking strength bonus. */
    public void setStrikingStrengthBonus(int bonus) {
        if (mStrikingStrengthBonus != bonus) {
            updateStrengthInfo(mStrength, mStrengthBonus, mLiftingStrengthBonus, bonus);
        }
    }

    private void updateStrengthInfo(int strength, int bonus, int liftingBonus, int strikingBonus) {
        Dice        thrust   = getThrust();
        Dice        swing    = getSwing();
        WeightValue lift     = getBasicLift();
        boolean     notifyST = mStrength != strength || mStrengthBonus != bonus;
        Dice        dice;

        mStrength = strength;
        mStrengthBonus = bonus;
        mLiftingStrengthBonus = liftingBonus;
        mStrikingStrengthBonus = strikingBonus;

        startNotify();
        if (notifyST) {
            notify(ID_STRENGTH, Integer.valueOf(getStrength()));
            notifyOfBaseHitPointChange();
        }
        WeightValue newLift = getBasicLift();
        if (!newLift.equals(lift)) {
            notify(ID_BASIC_LIFT, newLift);
            notify(ID_ONE_HANDED_LIFT, getOneHandedLift());
            notify(ID_TWO_HANDED_LIFT, getTwoHandedLift());
            notify(ID_SHOVE_AND_KNOCK_OVER, getShoveAndKnockOver());
            notify(ID_RUNNING_SHOVE_AND_KNOCK_OVER, getRunningShoveAndKnockOver());
            notify(ID_CARRY_ON_BACK, getCarryOnBack());
            notify(ID_SHIFT_SLIGHTLY, getShiftSlightly());
            for (Encumbrance encumbrance : Encumbrance.values()) {
                notify(MAXIMUM_CARRY_PREFIX + encumbrance.ordinal(), getMaximumCarry(encumbrance));
            }
        }

        dice = getThrust();
        if (!dice.equals(thrust)) {
            notify(ID_BASIC_THRUST, dice);
        }
        dice = getSwing();
        if (!dice.equals(swing)) {
            notify(ID_BASIC_SWING, dice);
        }

        updateSkills();
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on strength. */
    public int getStrengthPoints() {
        int reduction = mStrengthCostReduction;
        if (!SheetPreferences.areOptionalStrengthRulesUsed()) {
            reduction += mDescription.getSizeModifier() * 10;
        }
        return getPointsForAttribute(mStrength - 10, 10, reduction);
    }

    private static int getPointsForAttribute(int delta, int ptsPerLevel, int reduction) {
        int amt = delta * ptsPerLevel;
        if (reduction > 0 && delta > 0) {
            if (reduction > 80) {
                reduction = 80;
            }
            amt = (99 + amt * (100 - reduction)) / 100;
        }
        return amt;
    }

    /** @return The basic thrusting damage. */
    public Dice getThrust() {
        return getThrust(getStrength() + mStrikingStrengthBonus);
    }

    /**
     * @param strength The strength to return basic thrusting damage for.
     * @return The basic thrusting damage.
     */
    public static Dice getThrust(int strength) {
        if (SheetPreferences.areOptionalThrustDamageUsed()) {
            Dice dice = getSwing(strength);
            dice.add(-2);
            return dice;
        }
        if (SheetPreferences.areOptionalReducedSwingUsed()) {
            if (strength < 19) {
                return new Dice(1, -(6 - (strength - 1) / 2));
            }
            int dice = 1;
            int adds = (strength - 10) / 2 - 2;
            if ((strength - 10) % 2 == 1) {
                adds++;
            }
            dice += 2 * (adds / 7);
            adds %= 7;
            dice += adds / 4;
            adds %= 4;
            if (adds == 3) {
                dice++;
                adds = -1;
            }

            return new Dice(dice, adds);
        }

        if (SheetPreferences.areOptionalStrengthRulesUsed()) {
            if (strength < 12) {
                return new Dice(1, strength - 12);
            }
            return new Dice((strength - 7) / 4, (strength + 1) % 4 - 1);
        }

        int value = strength;

        if (value < 19) {
            return new Dice(1, -(6 - (value - 1) / 2));
        }

        value -= 11;
        if (strength > 50) {
            value--;
            if (strength > 79) {
                value -= 1 + (strength - 80) / 5;
            }
        }
        return new Dice(value / 8 + 1, value % 8 / 2 - 1);
    }

    /** @return The basic swinging damage. */
    public Dice getSwing() {
        return getSwing(getStrength() + mStrikingStrengthBonus);
    }

    /**
     * @param strength The strength to return basic swinging damage for.
     * @return The basic thrusting damage.
     */
    public static Dice getSwing(int strength) {
        if (SheetPreferences.areOptionalReducedSwingUsed()) {
            if (strength < 10) {
                return new Dice(1, -(5 - (strength - 1) / 2));
            }

            int dice = 1;
            int adds = (strength - 10) / 2;
            dice += 2 * (adds / 7);
            adds %= 7;
            dice += adds / 4;
            adds %= 4;
            if (adds == 3) {
                dice++;
                adds = -1;
            }

            return new Dice(dice, adds);
        }

        if (SheetPreferences.areOptionalStrengthRulesUsed()) {
            if (strength < 10) {
                return new Dice(1, strength - 10);
            }
            return new Dice((strength - 5) / 4, (strength - 1) % 4 - 1);
        }

        int value = strength;

        if (value < 10) {
            return new Dice(1, -(5 - (value - 1) / 2));
        }

        if (value < 28) {
            value -= 9;
            return new Dice(value / 4 + 1, value % 4 - 1);
        }

        if (strength > 40) {
            value -= (strength - 40) / 5;
        }

        if (strength > 59) {
            value++;
        }
        value += 9;
        return new Dice(value / 8 + 1, value % 8 / 2 - 1);
    }

    /** @return Basic lift. */
    public WeightValue getBasicLift() {
        return getBasicLift(DisplayPreferences.getWeightUnits());
    }

    private WeightValue getBasicLift(WeightUnits desiredUnits) {
        Fixed6      ten = new Fixed6(10);
        WeightUnits units;
        Fixed6      divisor;
        Fixed6      multiplier;
        Fixed6      roundAt;
        if (SheetPreferences.areGurpsMetricRulesUsed() && DisplayPreferences.getWeightUnits().isMetric()) {
            units = WeightUnits.KG;
            divisor = ten;
            multiplier = Fixed6.ONE;
            roundAt = new Fixed6(5);
        } else {
            units = WeightUnits.LB;
            divisor = new Fixed6(5);
            multiplier = new Fixed6(2);
            roundAt = ten;
        }
        int    strength = getStrength() + mLiftingStrengthBonus;
        Fixed6 value;
        if (strength < 1) {
            value = Fixed6.ZERO;
        } else {
            if (SheetPreferences.areOptionalStrengthRulesUsed()) {
                int diff = 0;
                if (strength > 19) {
                    diff = strength / 10 - 1;
                    strength -= diff * 10;
                }
                value = new Fixed6(Math.pow(10.0, strength / 10.0)).mul(multiplier);
                value = strength <= 6 ? value.mul(ten).round().div(ten) : value.round();
                value = value.mul(new Fixed6(Math.pow(10, diff)));
            } else {
                //noinspection UnnecessaryExplicitNumericCast
                value = new Fixed6((long) strength * (long) strength).div(divisor);
            }
            if (value.greaterThanOrEqual(roundAt)) {
                value = value.round();
            }
            value = value.mul(ten).trunc().div(ten);
        }
        return new WeightValue(desiredUnits.convert(units, value), desiredUnits);
    }

    private WeightValue getMultipleOfBasicLift(int multiple) {
        WeightValue lift = getBasicLift();
        lift.setValue(lift.getValue().mul(new Fixed6(multiple)));
        return lift;
    }

    /** @return The one-handed lift value. */
    public WeightValue getOneHandedLift() {
        return getMultipleOfBasicLift(2);
    }

    /** @return The two-handed lift value. */
    public WeightValue getTwoHandedLift() {
        return getMultipleOfBasicLift(8);
    }

    /** @return The shove and knock over value. */
    public WeightValue getShoveAndKnockOver() {
        return getMultipleOfBasicLift(12);
    }

    /** @return The running shove and knock over value. */
    public WeightValue getRunningShoveAndKnockOver() {
        return getMultipleOfBasicLift(24);
    }

    /** @return The carry on back value. */
    public WeightValue getCarryOnBack() {
        return getMultipleOfBasicLift(15);
    }

    /** @return The shift slightly value. */
    public WeightValue getShiftSlightly() {
        return getMultipleOfBasicLift(50);
    }

    /**
     * @param encumbrance The encumbrance level.
     * @return The maximum amount the character can carry for the specified encumbrance level.
     */
    public WeightValue getMaximumCarry(Encumbrance encumbrance) {
        WeightUnits calcUnits = SheetPreferences.areGurpsMetricRulesUsed() && DisplayPreferences.getWeightUnits().isMetric() ? WeightUnits.KG : WeightUnits.LB;
        WeightValue lift      = getBasicLift(calcUnits);
        lift.setValue(lift.getValue().mul(new Fixed6(encumbrance.getWeightMultiplier())));
        WeightUnits desiredUnits = DisplayPreferences.getWeightUnits();
        return new WeightValue(desiredUnits.convert(calcUnits, lift.getValue()), desiredUnits);
    }

    /**
     * @return The character's basic speed.
     */
    public double getBasicSpeed() {
        return mSpeed + mSpeedBonus + getRawBasicSpeed();
    }

    private double getRawBasicSpeed() {
        return (getDexterity() + getHealth()) / 4.0;
    }

    /**
     * Sets the basic speed.
     *
     * @param speed The new basic speed.
     */
    public void setBasicSpeed(double speed) {
        double oldBasicSpeed = getBasicSpeed();
        if (oldBasicSpeed != speed) {
            postUndoEdit(I18n.Text("Basic Speed Change"), ID_BASIC_SPEED, Double.valueOf(oldBasicSpeed), Double.valueOf(speed));
            updateBasicSpeedInfo(speed - (mSpeedBonus + getRawBasicSpeed()), mSpeedBonus);
        }
    }

    /** @return The basic speed bonus. */
    public double getBasicSpeedBonus() {
        return mSpeedBonus;
    }

    /** @param bonus The basic speed bonus. */
    public void setBasicSpeedBonus(double bonus) {
        if (mSpeedBonus != bonus) {
            updateBasicSpeedInfo(mSpeed, bonus);
        }
    }

    private void updateBasicSpeedInfo(double speed, double bonus) {
        int   move = getBasicMove();
        int[] data = preserveMoveAndDodge();
        int   tmp;

        mSpeed = speed;
        mSpeedBonus = bonus;

        startNotify();
        notify(ID_BASIC_SPEED, Double.valueOf(getBasicSpeed()));
        tmp = getBasicMove();
        if (move != tmp) {
            notify(ID_BASIC_MOVE, Integer.valueOf(tmp));
        }
        notifyIfMoveOrDodgeAltered(data);
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on basic speed. */
    public int getBasicSpeedPoints() {
        return (int) (mSpeed * 20.0);
    }

    /**
     * @return The character's basic move.
     */
    public int getBasicMove() {
        return Math.max(mMove + mMoveBonus + getRawBasicMove(), 0);
    }

    private int getRawBasicMove() {
        return (int) Math.floor(getBasicSpeed());
    }

    /**
     * Sets the basic move.
     *
     * @param move The new basic move.
     */
    public void setBasicMove(int move) {
        int oldBasicMove = getBasicMove();

        if (oldBasicMove != move) {
            postUndoEdit(I18n.Text("Basic Move Change"), ID_BASIC_MOVE, Integer.valueOf(oldBasicMove), Integer.valueOf(move));
            updateBasicMoveInfo(move - (mMoveBonus + getRawBasicMove()), mMoveBonus);
        }
    }

    /** @return The basic move bonus. */
    public int getBasicMoveBonus() {
        return mMoveBonus;
    }

    /** @param bonus The basic move bonus. */
    public void setBasicMoveBonus(int bonus) {
        if (mMoveBonus != bonus) {
            updateBasicMoveInfo(mMove, bonus);
        }
    }

    private void updateBasicMoveInfo(int move, int bonus) {
        int[] data = preserveMoveAndDodge();

        startNotify();
        mMove = move;
        mMoveBonus = bonus;
        notify(ID_BASIC_MOVE, Integer.valueOf(getBasicMove()));
        notifyIfMoveOrDodgeAltered(data);
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on basic move. */
    public int getBasicMovePoints() {
        return mMove * 5;
    }

    /**
     * @param encumbrance The encumbrance level.
     * @return The character's ground move for the specified encumbrance level.
     */
    public int getMove(Encumbrance encumbrance) {
        int basicMove = getBasicMove();
        if (isReeling() || isTired()) {
            basicMove /= 2;
        }
        int move = basicMove * (10 + 2 * encumbrance.getEncumbrancePenalty()) / 10;
        if (move < 1) {
            return basicMove > 0 ? 1 : 0;
        }
        return move;
    }

    /**
     * @param encumbrance The encumbrance level.
     * @return The character's dodge for the specified encumbrance level.
     */
    public int getDodge(Encumbrance encumbrance) {
        double basicSpeed = getBasicSpeed();
        if (isReeling() || isTired()) {
            basicSpeed /= 2;
        }
        return Math.max((int) Math.floor(basicSpeed) + 3 + encumbrance.getEncumbrancePenalty() + mDodgeBonus, 1);
    }

    /** @return The dodge bonus. */
    public int getDodgeBonus() {
        return mDodgeBonus;
    }

    /** @param bonus The dodge bonus. */
    public void setDodgeBonus(int bonus) {
        if (mDodgeBonus != bonus) {
            int[] data = preserveMoveAndDodge();

            mDodgeBonus = bonus;
            startNotify();
            notifySingle(ID_DODGE_BONUS, Integer.valueOf(mDodgeBonus));
            notifyIfMoveOrDodgeAltered(data);
            endNotify();
        }
    }

    /** @return The parry bonus. */
    public int getParryBonus() {
        return mParryBonus;
    }

    /** @param bonus The parry bonus. */
    public void setParryBonus(int bonus) {
        if (mParryBonus != bonus) {
            mParryBonus = bonus;
            notifySingle(ID_PARRY_BONUS, Integer.valueOf(mParryBonus));
        }
    }

    /** @return The block bonus. */
    public int getBlockBonus() {
        return mBlockBonus;
    }

    /** @param bonus The block bonus. */
    public void setBlockBonus(int bonus) {
        if (mBlockBonus != bonus) {
            mBlockBonus = bonus;
            notifySingle(ID_BLOCK_BONUS, Integer.valueOf(mBlockBonus));
        }
    }

    /** @return The current encumbrance level. */
    public Encumbrance getEncumbranceLevel() {
        Fixed6 carried = getWeightCarried().getNormalizedValue();
        for (Encumbrance encumbrance : Encumbrance.values()) {
            if (carried.lessThanOrEqual(getMaximumCarry(encumbrance).getNormalizedValue())) {
                return encumbrance;
            }
        }
        return Encumbrance.EXTRA_HEAVY;
    }

    /**
     * @return {@code true} if the carried weight is greater than the maximum allowed for an
     *         extra-heavy load.
     */
    public boolean isCarryingGreaterThanMaxLoad() {
        return getWeightCarried().getNormalizedValue().greaterThan(getMaximumCarry(Encumbrance.EXTRA_HEAVY).getNormalizedValue());
    }

    /** @return The current weight being carried. */
    public WeightValue getWeightCarried() {
        return mCachedWeightCarried;
    }

    /** @return The current wealth being carried. */
    public Fixed6 getWealthCarried() {
        return mCachedWealthCarried;
    }

    /** @return The current wealth not being carried. */
    public Fixed6 getWealthNotCarried() {
        return mCachedWealthNotCarried;
    }

    /**
     * Convert a metric {@link WeightValue} by GURPS Metric rules into an imperial one. If an
     * imperial {@link WeightValue} is passed as an argument, it will be returned unchanged.
     *
     * @param value The {@link WeightValue} to be converted by GURPS Metric rules.
     * @return The converted imperial {@link WeightValue}.
     */
    public static WeightValue convertFromGurpsMetric(WeightValue value) {
        switch (value.getUnits()) {
        case G:
            return new WeightValue(value.getValue().div(new Fixed6(30)), WeightUnits.OZ);
        case KG:
            return new WeightValue(value.getValue().mul(new Fixed6(2)), WeightUnits.LB);
        case T:
            return new WeightValue(value.getValue(), WeightUnits.LT);
        default:
            return value;
        }
    }

    /**
     * Convert an imperial {@link WeightValue} by GURPS Metric rules into a metric one. If a metric
     * {@link WeightValue} is passed as an argument, it will be returned unchanged.
     *
     * @param value The {@link WeightValue} to be converted by GURPS Metric rules.
     * @return The converted metric {@link WeightValue}.
     */
    public static WeightValue convertToGurpsMetric(WeightValue value) {
        switch (value.getUnits()) {
        case LB:
            return new WeightValue(value.getValue().div(new Fixed6(2)), WeightUnits.KG);
        case LT:
        case TN:
            return new WeightValue(value.getValue(), WeightUnits.T);
        case OZ:
            return new WeightValue(value.getValue().mul(new Fixed6(30)), WeightUnits.G);
        default:
            return value;
        }
    }

    /**
     * Calculate the total weight and wealth carried.
     *
     * @param notify Whether to send out notifications if the resulting values are different from
     *               the previous values.
     */
    public void calculateWeightAndWealthCarried(boolean notify) {
        WeightValue savedWeight = new WeightValue(mCachedWeightCarried);
        Fixed6      savedWealth = mCachedWealthCarried;
        mCachedWeightCarried = new WeightValue(Fixed6.ZERO, DisplayPreferences.getWeightUnits());
        mCachedWealthCarried = Fixed6.ZERO;
        for (Row one : mEquipment.getTopLevelRows()) {
            Equipment   equipment = (Equipment) one;
            WeightValue weight    = new WeightValue(equipment.getExtendedWeight());
            if (SheetPreferences.areGurpsMetricRulesUsed()) {
                weight = DisplayPreferences.getWeightUnits().isMetric() ? convertToGurpsMetric(weight) : convertFromGurpsMetric(weight);
            }
            mCachedWeightCarried.add(weight);
            mCachedWealthCarried = mCachedWealthCarried.add(equipment.getExtendedValue());
        }
        if (notify) {
            if (!savedWeight.equals(mCachedWeightCarried)) {
                notify(ID_CARRIED_WEIGHT, mCachedWeightCarried);
            }
            if (!mCachedWealthCarried.equals(savedWealth)) {
                notify(ID_CARRIED_WEALTH, mCachedWealthCarried);
            }
        }
    }

    /**
     * Calculate the total wealth not carried.
     *
     * @param notify Whether to send out notifications if the resulting values are different from
     *               the previous values.
     */
    public void calculateWealthNotCarried(boolean notify) {
        Fixed6 savedWealth = mCachedWealthNotCarried;
        mCachedWealthNotCarried = Fixed6.ZERO;
        for (Row one : mOtherEquipment.getTopLevelRows()) {
            mCachedWealthNotCarried = mCachedWealthNotCarried.add(((Equipment) one).getExtendedValue());
        }
        if (notify) {
            if (!mCachedWealthNotCarried.equals(savedWealth)) {
                notify(ID_NOT_CARRIED_WEALTH, mCachedWealthNotCarried);
            }
        }
    }

    private int[] preserveMoveAndDodge() {
        Encumbrance[] values = Encumbrance.values();
        int[]         data   = new int[values.length * 2];
        for (Encumbrance encumbrance : values) {
            int index = encumbrance.ordinal();
            data[index] = getMove(encumbrance);
            data[values.length + index] = getDodge(encumbrance);
        }
        return data;
    }

    private void notifyIfMoveOrDodgeAltered(int[] data) {
        Encumbrance[] values = Encumbrance.values();
        for (Encumbrance encumbrance : values) {
            int index = encumbrance.ordinal();
            int tmp   = getDodge(encumbrance);
            if (tmp != data[values.length + index]) {
                notify(DODGE_PREFIX + index, Integer.valueOf(tmp));
            }
            tmp = getMove(encumbrance);
            if (tmp != data[index]) {
                notify(MOVE_PREFIX + index, Integer.valueOf(tmp));
            }
        }
    }

    public void notifyMoveAndDodge() {
        for (Encumbrance encumbrance : Encumbrance.values()) {
            int index = encumbrance.ordinal();
            notify(DODGE_PREFIX + index, Integer.valueOf(getDodge(encumbrance)));
            notify(MOVE_PREFIX + index, Integer.valueOf(getMove(encumbrance)));
        }
    }

    /** @return The dexterity (DX). */
    public int getDexterity() {
        return mDexterity + mDexterityBonus;
    }

    /**
     * Sets the dexterity (DX).
     *
     * @param dexterity The new dexterity.
     */
    public void setDexterity(int dexterity) {
        int oldDexterity = getDexterity();

        if (oldDexterity != dexterity) {
            postUndoEdit(I18n.Text("Dexterity Change"), ID_DEXTERITY, Integer.valueOf(oldDexterity), Integer.valueOf(dexterity));
            updateDexterityInfo(dexterity - mDexterityBonus, mDexterityBonus);
        }
    }

    /** @return The dexterity bonus. */
    public int getDexterityBonus() {
        return mDexterityBonus;
    }

    /** @param bonus The new dexterity bonus. */
    public void setDexterityBonus(int bonus) {
        if (mDexterityBonus != bonus) {
            updateDexterityInfo(mDexterity, bonus);
        }
    }

    /** @param reduction The cost reduction for dexterity. */
    public void setDexterityCostReduction(int reduction) {
        if (mDexterityCostReduction != reduction) {
            mDexterityCostReduction = reduction;
            mNeedAttributePointCalculation = true;
        }
    }

    private void updateDexterityInfo(int dexterity, int bonus) {
        double speed = getBasicSpeed();
        int    move  = getBasicMove();
        int[]  data  = preserveMoveAndDodge();
        double newSpeed;
        int    newMove;

        mDexterity = dexterity;
        mDexterityBonus = bonus;

        startNotify();
        notify(ID_DEXTERITY, Integer.valueOf(getDexterity()));
        newSpeed = getBasicSpeed();
        if (newSpeed != speed) {
            notify(ID_BASIC_SPEED, Double.valueOf(newSpeed));
        }
        newMove = getBasicMove();
        if (newMove != move) {
            notify(ID_BASIC_MOVE, Integer.valueOf(newMove));
        }
        notifyIfMoveOrDodgeAltered(data);
        updateSkills();
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on dexterity. */
    public int getDexterityPoints() {
        return getPointsForAttribute(mDexterity - 10, 20, mDexterityCostReduction);
    }

    /** @return The intelligence (IQ). */
    public int getIntelligence() {
        return mIntelligence + mIntelligenceBonus;
    }

    /**
     * Sets the intelligence (IQ).
     *
     * @param intelligence The new intelligence.
     */
    public void setIntelligence(int intelligence) {
        int oldIntelligence = getIntelligence();
        if (oldIntelligence != intelligence) {
            postUndoEdit(I18n.Text("Intelligence Change"), ID_INTELLIGENCE, Integer.valueOf(oldIntelligence), Integer.valueOf(intelligence));
            updateIntelligenceInfo(intelligence - mIntelligenceBonus, mIntelligenceBonus);
        }
    }

    /** @return The intelligence bonus. */
    public int getIntelligenceBonus() {
        return mIntelligenceBonus;
    }

    /** @param bonus The new intelligence bonus. */
    public void setIntelligenceBonus(int bonus) {
        if (mIntelligenceBonus != bonus) {
            updateIntelligenceInfo(mIntelligence, bonus);
        }
    }

    /** @param reduction The cost reduction for intelligence. */
    public void setIntelligenceCostReduction(int reduction) {
        if (mIntelligenceCostReduction != reduction) {
            mIntelligenceCostReduction = reduction;
            mNeedAttributePointCalculation = true;
        }
    }

    private void updateIntelligenceInfo(int intelligence, int bonus) {
        int perception = getPerception();
        int will       = getWill();
        int newPerception;
        int newWill;

        mIntelligence = intelligence;
        mIntelligenceBonus = bonus;

        startNotify();
        notify(ID_INTELLIGENCE, Integer.valueOf(getIntelligence()));
        newPerception = getPerception();
        if (newPerception != perception) {
            notify(ID_PERCEPTION, Integer.valueOf(newPerception));
            notify(ID_VISION, Integer.valueOf(getVision()));
            notify(ID_HEARING, Integer.valueOf(getHearing()));
            notify(ID_TASTE_AND_SMELL, Integer.valueOf(getTasteAndSmell()));
            notify(ID_TOUCH, Integer.valueOf(getTouch()));
        }
        newWill = getWill();
        if (newWill != will) {
            notify(ID_WILL, Integer.valueOf(newWill));
            notify(ID_FRIGHT_CHECK, Integer.valueOf(getFrightCheck()));
        }
        updateSkills();
        updateSpells();
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on intelligence. */
    public int getIntelligencePoints() {
        return getPointsForAttribute(mIntelligence - 10, 20, mIntelligenceCostReduction);
    }

    /** @return The health (HT). */
    public int getHealth() {
        return mHealth + mHealthBonus;
    }

    /**
     * Sets the health (HT).
     *
     * @param health The new health.
     */
    public void setHealth(int health) {
        int oldHealth = getHealth();

        if (oldHealth != health) {
            postUndoEdit(I18n.Text("Health Change"), ID_HEALTH, Integer.valueOf(oldHealth), Integer.valueOf(health));
            updateHealthInfo(health - mHealthBonus, mHealthBonus);
        }
    }

    /** @return The health bonus. */
    public int getHealthBonus() {
        return mHealthBonus;
    }

    /** @param bonus The new health bonus. */
    public void setHealthBonus(int bonus) {
        if (mHealthBonus != bonus) {
            updateHealthInfo(mHealth, bonus);
        }
    }

    /** @param reduction The cost reduction for health. */
    public void setHealthCostReduction(int reduction) {
        if (mHealthCostReduction != reduction) {
            mHealthCostReduction = reduction;
            mNeedAttributePointCalculation = true;
        }
    }

    private void updateHealthInfo(int health, int bonus) {
        double speed = getBasicSpeed();
        int    move  = getBasicMove();
        int[]  data  = preserveMoveAndDodge();
        double newSpeed;
        int    tmp;

        mHealth = health;
        mHealthBonus = bonus;

        startNotify();
        notify(ID_HEALTH, Integer.valueOf(getHealth()));

        newSpeed = getBasicSpeed();
        if (newSpeed != speed) {
            notify(ID_BASIC_SPEED, Double.valueOf(newSpeed));
        }

        tmp = getBasicMove();
        if (tmp != move) {
            notify(ID_BASIC_MOVE, Integer.valueOf(tmp));
        }
        notifyIfMoveOrDodgeAltered(data);
        notifyOfBaseFatiguePointChange();
        updateSkills();
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on health. */
    public int getHealthPoints() {
        return getPointsForAttribute(mHealth - 10, 10, mHealthCostReduction);
    }

    /** @return The total number of points this character has. */
    public int getTotalPoints() {
        return mTotalPoints;
    }

    /** @return The total number of points spent. */
    public int getSpentPoints() {
        return getAttributePoints() + getAdvantagePoints() + getDisadvantagePoints() + getQuirkPoints() + getSkillPoints() + getSpellPoints() + getRacePoints();
    }

    /** @return The number of unspent points. */
    public int getUnspentPoints() {
        return mTotalPoints - getSpentPoints();
    }

    /**
     * Sets the unspent character points.
     *
     * @param unspent The new unspent character points.
     */
    public void setUnspentPoints(int unspent) {
        int current = getUnspentPoints();

        if (current != unspent) {
            Integer value = Integer.valueOf(unspent);

            postUndoEdit(I18n.Text("Unspent Points Change"), ID_UNSPENT_POINTS, Integer.valueOf(current), value);
            mTotalPoints = unspent + getSpentPoints();
            startNotify();
            notify(ID_UNSPENT_POINTS, value);
            notify(ID_TOTAL_POINTS, Integer.valueOf(getTotalPoints()));
            endNotify();
        }
    }

    /** @return The number of points spent on basic attributes. */
    public int getAttributePoints() {
        return mCachedAttributePoints;
    }

    private void calculateAttributePoints() {
        mCachedAttributePoints = getStrengthPoints() + getDexterityPoints() + getIntelligencePoints() + getHealthPoints() + getWillPoints() + getPerceptionPoints() + getBasicSpeedPoints() + getBasicMovePoints() + getHitPointPoints() + getFatiguePointPoints();
    }

    /** @return The number of points spent on a racial package. */
    public int getRacePoints() {
        return mCachedRacePoints;
    }

    /** @return The number of points spent on advantages. */
    public int getAdvantagePoints() {
        return mCachedAdvantagePoints;
    }

    /** @return The number of points spent on disadvantages. */
    public int getDisadvantagePoints() {
        return mCachedDisadvantagePoints;
    }

    /** @return The number of points spent on quirks. */
    public int getQuirkPoints() {
        return mCachedQuirkPoints;
    }

    private void calculateAdvantagePoints() {
        mCachedAdvantagePoints = 0;
        mCachedDisadvantagePoints = 0;
        mCachedRacePoints = 0;
        mCachedQuirkPoints = 0;
        for (Advantage advantage : new FilteredIterator<>(mAdvantages.getTopLevelRows(), Advantage.class)) {
            calculateSingleAdvantagePoints(advantage);
        }
    }

    private void calculateSingleAdvantagePoints(Advantage advantage) {
        if (advantage.canHaveChildren()) {
            AdvantageContainerType type = advantage.getContainerType();
            if (type == AdvantageContainerType.GROUP) {
                for (Advantage child : new FilteredIterator<>(advantage.getChildren(), Advantage.class)) {
                    calculateSingleAdvantagePoints(child);
                }
                return;
            } else if (type == AdvantageContainerType.RACE) {
                mCachedRacePoints += advantage.getAdjustedPoints();
                return;
            }
        }

        int pts = advantage.getAdjustedPoints();
        if (pts > 0) {
            mCachedAdvantagePoints += pts;
        } else if (pts < -1) {
            mCachedDisadvantagePoints += pts;
        } else if (pts == -1) {
            mCachedQuirkPoints--;
        }
    }

    /** @return The number of points spent on skills. */
    public int getSkillPoints() {
        return mCachedSkillPoints;
    }

    private void calculateSkillPoints() {
        mCachedSkillPoints = 0;
        for (Skill skill : getSkillsIterator()) {
            if (!skill.canHaveChildren()) {
                mCachedSkillPoints += skill.getPoints();
            }
        }
    }

    /** @return The number of points spent on spells. */
    public int getSpellPoints() {
        return mCachedSpellPoints;
    }

    private void calculateSpellPoints() {
        mCachedSpellPoints = 0;
        for (Spell spell : getSpellsIterator()) {
            if (!spell.canHaveChildren()) {
                mCachedSpellPoints += spell.getPoints();
            }
        }
    }

    /** @return Whether to include the punch natural weapon or not. */
    public boolean includePunch() {
        return mIncludePunch;
    }

    /** @param include Whether to include the punch natural weapon or not. */
    public void setIncludePunch(boolean include) {
        if (mIncludePunch != include) {
            postUndoEdit(I18n.Text("Include Punch In Weapons"), ID_INCLUDE_PUNCH, Boolean.valueOf(mIncludePunch), Boolean.valueOf(include));
            mIncludePunch = include;
            notifySingle(ID_INCLUDE_PUNCH, Boolean.valueOf(mIncludePunch));
        }
    }

    /** @return Whether to include the kick natural weapon or not. */
    public boolean includeKick() {
        return mIncludeKick;
    }

    /** @param include Whether to include the kick natural weapon or not. */
    public void setIncludeKick(boolean include) {
        if (mIncludeKick != include) {
            postUndoEdit(I18n.Text("Include Kick In Weapons"), ID_INCLUDE_KICK, Boolean.valueOf(mIncludeKick), Boolean.valueOf(include));
            mIncludeKick = include;
            notifySingle(ID_INCLUDE_KICK, Boolean.valueOf(mIncludeKick));
        }
    }

    /** @return Whether to include the kick w/boots natural weapon or not. */
    public boolean includeKickBoots() {
        return mIncludeKickBoots;
    }

    /** @param include Whether to include the kick w/boots natural weapon or not. */
    public void setIncludeKickBoots(boolean include) {
        if (mIncludeKickBoots != include) {
            postUndoEdit(I18n.Text("Include Kick w/Boots In Weapons"), ID_INCLUDE_BOOTS, Boolean.valueOf(mIncludeKickBoots), Boolean.valueOf(include));
            mIncludeKickBoots = include;
            notifySingle(ID_INCLUDE_BOOTS, Boolean.valueOf(mIncludeKickBoots));
        }
    }

    public int getCurrentHitPoints() {
        return getHitPoints() - getHitPointsDamage();
    }

    /** @return The hit points (HP). */
    public int getHitPoints() {
        return getStrength() + mHitPoints + mHitPointBonus;
    }

    /**
     * Sets the hit points (HP).
     *
     * @param hp The new hit points.
     */
    public void setHitPoints(int hp) {
        int oldHP = getHitPoints();
        if (oldHP != hp) {
            postUndoEdit(I18n.Text("Hit Points Change"), ID_HIT_POINTS, Integer.valueOf(oldHP), Integer.valueOf(hp));
            startNotify();
            mHitPoints = hp - (getStrength() + mHitPointBonus);
            mNeedAttributePointCalculation = true;
            notifyOfBaseHitPointChange();
            endNotify();
        }
    }

    /** @return The number of points spent on hit points. */
    public int getHitPointPoints() {
        int pts = 2 * mHitPoints;
        if (!SheetPreferences.areOptionalStrengthRulesUsed()) {
            int sizeModifier = mDescription.getSizeModifier();
            if (sizeModifier > 0) {
                int rem;
                if (sizeModifier > 8) {
                    sizeModifier = 8;
                }
                pts *= 10 - sizeModifier;
                rem = pts % 10;
                pts /= 10;
                if (rem > 4) {
                    pts++;
                } else if (rem < -5) {
                    pts--;
                }
            }
        }
        return pts;
    }

    /** @return The hit point bonus. */
    public int getHitPointBonus() {
        return mHitPointBonus;
    }

    /** @param bonus The hit point bonus. */
    public void setHitPointBonus(int bonus) {
        if (mHitPointBonus != bonus) {
            mHitPointBonus = bonus;
            notifyOfBaseHitPointChange();
        }
    }

    private void notifyOfBaseHitPointChange() {
        startNotify();
        notify(ID_HIT_POINTS, Integer.valueOf(getHitPoints()));
        notify(ID_DEATH_CHECK_1_HIT_POINTS, Integer.valueOf(getDeathCheck1HitPoints()));
        notify(ID_DEATH_CHECK_2_HIT_POINTS, Integer.valueOf(getDeathCheck2HitPoints()));
        notify(ID_DEATH_CHECK_3_HIT_POINTS, Integer.valueOf(getDeathCheck3HitPoints()));
        notify(ID_DEATH_CHECK_4_HIT_POINTS, Integer.valueOf(getDeathCheck4HitPoints()));
        notify(ID_DEAD_HIT_POINTS, Integer.valueOf(getDeadHitPoints()));
        notify(ID_REELING_HIT_POINTS, Integer.valueOf(getReelingHitPoints()));
        notify(ID_CURRENT_HP, Integer.valueOf(getHitPoints() - mHitPointsDamage));
        endNotify();
    }

    /** @return The hit points damage. */
    public int getHitPointsDamage() {
        return mHitPointsDamage;
    }

    /**
     * Sets the hit points damage.
     *
     * @param damage The damage amount.
     */
    public void setHitPointsDamage(int damage) {
        if (mHitPointsDamage != damage) {
            postUndoEdit(I18n.Text("Current Hit Points Change"), ID_HIT_POINTS_DAMAGE, Integer.valueOf(mHitPointsDamage), Integer.valueOf(damage));
            mHitPointsDamage = damage;
            notifySingle(ID_HIT_POINTS_DAMAGE, Integer.valueOf(mHitPointsDamage));
            notifySingle(ID_CURRENT_HP, Integer.valueOf(getHitPoints() - mHitPointsDamage));
        }
    }

    /** @return The number of hit points where "reeling" effects start. */
    public int getReelingHitPoints() {
        return Math.max((getHitPoints() - 1) / 3, 0);
    }

    public boolean isReeling() {
        return getCurrentHitPoints() <= getReelingHitPoints();
    }

    /** @return The number of hit points where unconsciousness checks must start being made. */
    @SuppressWarnings("static-method")
    public int getUnconsciousChecksHitPoints() {
        return 0;
    }

    /** @return The number of hit points where the first death check must be made. */
    public int getDeathCheck1HitPoints() {
        return -1 * getHitPoints();
    }

    /** @return The number of hit points where the second death check must be made. */
    public int getDeathCheck2HitPoints() {
        return -2 * getHitPoints();
    }

    /** @return The number of hit points where the third death check must be made. */
    public int getDeathCheck3HitPoints() {
        return -3 * getHitPoints();
    }

    /** @return The number of hit points where the fourth death check must be made. */
    public int getDeathCheck4HitPoints() {
        return -4 * getHitPoints();
    }

    /** @return The number of hit points where the character is just dead. */
    public int getDeadHitPoints() {
        return -5 * getHitPoints();
    }

    /** @return The will. */
    public int getWill() {
        return mWill + mWillBonus + (SheetPreferences.areOptionalIQRulesUsed() ? 10 : getIntelligence());
    }

    /** @param will The new will. */
    public void setWill(int will) {
        int oldWill = getWill();
        if (oldWill != will) {
            postUndoEdit(I18n.Text("Will Change"), ID_WILL, Integer.valueOf(oldWill), Integer.valueOf(will));
            updateWillInfo(will - (mWillBonus + (SheetPreferences.areOptionalIQRulesUsed() ? 10 : getIntelligence())), mWillBonus);
        }
    }

    /** @return The will bonus. */
    public int getWillBonus() {
        return mWillBonus;
    }

    /** @param bonus The new will bonus. */
    public void setWillBonus(int bonus) {
        if (mWillBonus != bonus) {
            updateWillInfo(mWill, bonus);
        }
    }

    private void updateWillInfo(int will, int bonus) {
        mWill = will;
        mWillBonus = bonus;

        startNotify();
        notify(ID_WILL, Integer.valueOf(getWill()));
        notify(ID_FRIGHT_CHECK, Integer.valueOf(getFrightCheck()));
        updateSkills();
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** Called to ensure notifications are sent out when the optional IQ rule use is changed. */
    public void updateWillAndPerceptionDueToOptionalIQRuleUseChange() {
        updateWillInfo(mWill, mWillBonus);
        updatePerceptionInfo(mPerception, mPerceptionBonus);
    }

    /** @return The number of points spent on will. */
    public int getWillPoints() {
        return mWill * 5;
    }

    /** @return The fright check. */
    public int getFrightCheck() {
        return getWill() + mFrightCheckBonus;
    }

    /** @return The fright check bonus. */
    public int getFrightCheckBonus() {
        return mFrightCheckBonus;
    }

    /** @param bonus The new fright check bonus. */
    public void setFrightCheckBonus(int bonus) {
        if (mFrightCheckBonus != bonus) {
            mFrightCheckBonus = bonus;
            startNotify();
            notify(ID_FRIGHT_CHECK, Integer.valueOf(getFrightCheck()));
            endNotify();
        }
    }

    /** @return The vision. */
    public int getVision() {
        return getPerception() + mVisionBonus;
    }

    /** @return The vision bonus. */
    public int getVisionBonus() {
        return mVisionBonus;
    }

    /** @param bonus The new vision bonus. */
    public void setVisionBonus(int bonus) {
        if (mVisionBonus != bonus) {
            mVisionBonus = bonus;
            startNotify();
            notify(ID_VISION, Integer.valueOf(getVision()));
            endNotify();
        }
    }

    /** @return The hearing. */
    public int getHearing() {
        return getPerception() + mHearingBonus;
    }

    /** @return The hearing bonus. */
    public int getHearingBonus() {
        return mHearingBonus;
    }

    /** @param bonus The new hearing bonus. */
    public void setHearingBonus(int bonus) {
        if (mHearingBonus != bonus) {
            mHearingBonus = bonus;
            startNotify();
            notify(ID_HEARING, Integer.valueOf(getHearing()));
            endNotify();
        }
    }

    /** @return The touch perception. */
    public int getTouch() {
        return getPerception() + mTouchBonus;
    }

    /** @return The touch bonus. */
    public int getTouchBonus() {
        return mTouchBonus;
    }

    /** @param bonus The new touch bonus. */
    public void setTouchBonus(int bonus) {
        if (mTouchBonus != bonus) {
            mTouchBonus = bonus;
            startNotify();
            notify(ID_TOUCH, Integer.valueOf(getTouch()));
            endNotify();
        }
    }

    /** @return The taste and smell perception. */
    public int getTasteAndSmell() {
        return getPerception() + mTasteAndSmellBonus;
    }

    /** @return The taste and smell bonus. */
    public int getTasteAndSmellBonus() {
        return mTasteAndSmellBonus;
    }

    /** @param bonus The new taste and smell bonus. */
    public void setTasteAndSmellBonus(int bonus) {
        if (mTasteAndSmellBonus != bonus) {
            mTasteAndSmellBonus = bonus;
            startNotify();
            notify(ID_TASTE_AND_SMELL, Integer.valueOf(getTasteAndSmell()));
            endNotify();
        }
    }

    /** @return The perception (Per). */
    public int getPerception() {
        return mPerception + mPerceptionBonus + (SheetPreferences.areOptionalIQRulesUsed() ? 10 : getIntelligence());
    }

    /**
     * Sets the perception.
     *
     * @param perception The new perception.
     */
    public void setPerception(int perception) {
        int oldPerception = getPerception();
        if (oldPerception != perception) {
            postUndoEdit(I18n.Text("Perception Change"), ID_PERCEPTION, Integer.valueOf(oldPerception), Integer.valueOf(perception));
            updatePerceptionInfo(perception - (mPerceptionBonus + (SheetPreferences.areOptionalIQRulesUsed() ? 10 : getIntelligence())), mPerceptionBonus);
        }
    }

    /** @return The perception bonus. */
    public int getPerceptionBonus() {
        return mPerceptionBonus;
    }

    /** @param bonus The new perception bonus. */
    public void setPerceptionBonus(int bonus) {
        if (mPerceptionBonus != bonus) {
            updatePerceptionInfo(mPerception, bonus);
        }
    }

    private void updatePerceptionInfo(int perception, int bonus) {
        mPerception = perception;
        mPerceptionBonus = bonus;
        startNotify();
        notify(ID_PERCEPTION, Integer.valueOf(getPerception()));
        notify(ID_VISION, Integer.valueOf(getVision()));
        notify(ID_HEARING, Integer.valueOf(getHearing()));
        notify(ID_TASTE_AND_SMELL, Integer.valueOf(getTasteAndSmell()));
        notify(ID_TOUCH, Integer.valueOf(getTouch()));
        updateSkills();
        mNeedAttributePointCalculation = true;
        endNotify();
    }

    /** @return The number of points spent on perception. */
    public int getPerceptionPoints() {
        return mPerception * 5;
    }

    public int getCurrentFatiguePoints() {
        return getFatiguePoints() - getFatiguePointsDamage();
    }

    /** @return The fatigue points (FP). */
    public int getFatiguePoints() {
        return getHealth() + mFatiguePoints + mFatiguePointBonus;
    }

    /**
     * Sets the fatigue points (HP).
     *
     * @param fp The new fatigue points.
     */
    public void setFatiguePoints(int fp) {
        int oldFP = getFatiguePoints();
        if (oldFP != fp) {
            postUndoEdit(I18n.Text("Fatigue Points Change"), ID_FATIGUE_POINTS, Integer.valueOf(oldFP), Integer.valueOf(fp));
            startNotify();
            mFatiguePoints = fp - (getHealth() + mFatiguePointBonus);
            mNeedAttributePointCalculation = true;
            notifyOfBaseFatiguePointChange();
            endNotify();
        }
    }

    /** @return The number of points spent on fatigue points. */
    public int getFatiguePointPoints() {
        return 3 * mFatiguePoints;
    }

    /** @return The fatigue point bonus. */
    public int getFatiguePointBonus() {
        return mFatiguePointBonus;
    }

    /** @param bonus The fatigue point bonus. */
    public void setFatiguePointBonus(int bonus) {
        if (mFatiguePointBonus != bonus) {
            mFatiguePointBonus = bonus;
            notifyOfBaseFatiguePointChange();
        }
    }

    private void notifyOfBaseFatiguePointChange() {
        startNotify();
        notify(ID_FATIGUE_POINTS, Integer.valueOf(getFatiguePoints()));
        notify(ID_UNCONSCIOUS_CHECKS_FATIGUE_POINTS, Integer.valueOf(getUnconsciousChecksFatiguePoints()));
        notify(ID_UNCONSCIOUS_FATIGUE_POINTS, Integer.valueOf(getUnconsciousFatiguePoints()));
        notify(ID_TIRED_FATIGUE_POINTS, Integer.valueOf(getTiredFatiguePoints()));
        notify(ID_CURRENT_FP, Integer.valueOf(getFatiguePoints() - mFatiguePointsDamage));
        endNotify();
    }

    /** @return The fatigue points damage. */
    public int getFatiguePointsDamage() {
        return mFatiguePointsDamage;
    }

    /**
     * Sets the fatigue points damage.
     *
     * @param damage The damage amount.
     */
    public void setFatiguePointsDamage(int damage) {
        if (mFatiguePointsDamage != damage) {
            postUndoEdit(I18n.Text("Current Fatigue Points Change"), ID_FATIGUE_POINTS_DAMAGE, Integer.valueOf(mFatiguePointsDamage), Integer.valueOf(damage));
            mFatiguePointsDamage = damage;
            notifySingle(ID_FATIGUE_POINTS_DAMAGE, Integer.valueOf(mFatiguePointsDamage));
            notifySingle(ID_CURRENT_FP, Integer.valueOf(getFatiguePoints() - mFatiguePointsDamage));
        }
    }

    /** @return The number of fatigue points where "tired" effects start. */
    public int getTiredFatiguePoints() {
        return Math.max((getFatiguePoints() - 1) / 3, 0);
    }

    public boolean isTired() {
        return getCurrentFatiguePoints() <= getTiredFatiguePoints();
    }

    /** @return The number of fatigue points where unconsciousness checks must start being made. */
    @SuppressWarnings("static-method")
    public int getUnconsciousChecksFatiguePoints() {
        return 0;
    }

    /** @return The number of hit points where the character falls over, unconscious. */
    public int getUnconsciousFatiguePoints() {
        return -1 * getFatiguePoints();
    }

    /** @return The {@link Profile} data. */
    public Profile getDescription() {
        return mDescription;
    }

    /** @return The {@link Armor} stats. */
    public Armor getArmor() {
        return mArmor;
    }

    /** @return The outline model for the character's advantages. */
    public OutlineModel getAdvantagesModel() {
        return mAdvantages;
    }

    /**
     * @param includeDisabled {@code true} if disabled entries should be included.
     * @return A recursive iterator over the character's advantages.
     */
    public RowIterator<Advantage> getAdvantagesIterator(boolean includeDisabled) {
        if (includeDisabled) {
            return new RowIterator<>(mAdvantages);
        }
        return new RowIterator<>(mAdvantages, (row) -> row.isEnabled());
    }

    /**
     * Searches the character's current advantages list for the specified name.
     *
     * @param name The name to look for.
     * @return The advantage, if present, or {@code null}.
     */
    public Advantage getAdvantageNamed(String name) {
        for (Advantage advantage : getAdvantagesIterator(false)) {
            if (advantage.getName().equals(name)) {
                return advantage;
            }
        }
        return null;
    }

    /**
     * Searches the character's current advantages list for the specified name.
     *
     * @param name The name to look for.
     * @return Whether it is present or not.
     */
    public boolean hasAdvantageNamed(String name) {
        return getAdvantageNamed(name) != null;
    }

    /** @return The outline model for the character's skills. */
    public OutlineModel getSkillsRoot() {
        return mSkills;
    }

    /** @return A recursive iterable for the character's skills. */
    public RowIterator<Skill> getSkillsIterator() {
        return new RowIterator<>(mSkills);
    }

    /**
     * Searches the character's current skill list for the specified name.
     *
     * @param name           The name to look for.
     * @param specialization The specialization to look for. Pass in {@code null} or an empty string
     *                       to ignore.
     * @param requirePoints  Only look at {@link Skill}s that have points. {@link Technique}s,
     *                       however, still won't need points even if this is {@code true}.
     * @param excludes       The set of {@link Skill}s to exclude from consideration.
     * @return The skill if it is present, or {@code null} if its not.
     */
    public List<Skill> getSkillNamed(String name, String specialization, boolean requirePoints, Set<String> excludes) {
        List<Skill> skills              = new ArrayList<>();
        boolean     checkSpecialization = specialization != null && !specialization.isEmpty();
        for (Skill skill : getSkillsIterator()) {
            if (!skill.canHaveChildren()) {
                if (excludes == null || !excludes.contains(skill.toString())) {
                    if (!requirePoints || skill instanceof Technique || skill.getPoints() > 0) {
                        if (skill.getName().equalsIgnoreCase(name)) {
                            if (!checkSpecialization || skill.getSpecialization().equalsIgnoreCase(specialization)) {
                                skills.add(skill);
                            }
                        }
                    }
                }
            }
        }
        return skills;
    }

    /**
     * Searches the character's current {@link Skill} list for the {@link Skill} with the best level
     * that matches the name.
     *
     * @param name           The {@link Skill} name to look for.
     * @param specialization An optional specialization to look for. Pass {@code null} if it is not
     *                       needed.
     * @param requirePoints  Only look at {@link Skill}s that have points. {@link Technique}s,
     *                       however, still won't need points even if this is {@code true}.
     * @param excludes       The set of {@link Skill}s to exclude from consideration.
     * @return The {@link Skill} that matches with the highest level.
     */
    public Skill getBestSkillNamed(String name, String specialization, boolean requirePoints, Set<String> excludes) {
        Skill best  = null;
        int   level = Integer.MIN_VALUE;
        for (Skill skill : getSkillNamed(name, specialization, requirePoints, excludes)) {
            int skillLevel = skill.getLevel(excludes);
            if (best == null || skillLevel > level) {
                best = skill;
                level = skillLevel;
            }
        }
        return best;
    }

    /** @return The outline model for the character's spells. */
    public OutlineModel getSpellsRoot() {
        return mSpells;
    }

    /** @return A recursive iterator over the character's spells. */
    public RowIterator<Spell> getSpellsIterator() {
        return new RowIterator<>(mSpells);
    }

    /** @return The outline model for the character's equipment. */
    public OutlineModel getEquipmentRoot() {
        return mEquipment;
    }

    /** @return A recursive iterator over the character's equipment. */
    public RowIterator<Equipment> getEquipmentIterator() {
        return new RowIterator<>(mEquipment);
    }

    /** @return The outline model for the character's other equipment. */
    public OutlineModel getOtherEquipmentRoot() {
        return mOtherEquipment;
    }

    /** @return A recursive iterator over the character's other equipment. */
    public RowIterator<Equipment> getOtherEquipmentIterator() {
        return new RowIterator<>(mOtherEquipment);
    }

    /** @return The outline model for the character's notes. */
    public OutlineModel getNotesRoot() {
        return mNotes;
    }

    /** @return A recursive iterator over the character's notes. */
    public RowIterator<Note> getNoteIterator() {
        return new RowIterator<>(mNotes);
    }

    /** @param map The new feature map. */
    public void setFeatureMap(HashMap<String, ArrayList<Feature>> map) {
        mFeatureMap = map;
        mSkillsUpdated = false;
        mSpellsUpdated = false;

        startNotify();
        setStrengthBonus(getIntegerBonusFor(ID_STRENGTH));
        setStrengthCostReduction(getCostReductionFor(ID_STRENGTH));
        setLiftingStrengthBonus(getIntegerBonusFor(ID_LIFTING_STRENGTH));
        setStrikingStrengthBonus(getIntegerBonusFor(ID_STRIKING_STRENGTH));
        setDexterityBonus(getIntegerBonusFor(ID_DEXTERITY));
        setDexterityCostReduction(getCostReductionFor(ID_DEXTERITY));
        setIntelligenceBonus(getIntegerBonusFor(ID_INTELLIGENCE));
        setIntelligenceCostReduction(getCostReductionFor(ID_INTELLIGENCE));
        setHealthBonus(getIntegerBonusFor(ID_HEALTH));
        setHealthCostReduction(getCostReductionFor(ID_HEALTH));
        setWillBonus(getIntegerBonusFor(ID_WILL));
        setFrightCheckBonus(getIntegerBonusFor(ID_FRIGHT_CHECK));
        setPerceptionBonus(getIntegerBonusFor(ID_PERCEPTION));
        setVisionBonus(getIntegerBonusFor(ID_VISION));
        setHearingBonus(getIntegerBonusFor(ID_HEARING));
        setTasteAndSmellBonus(getIntegerBonusFor(ID_TASTE_AND_SMELL));
        setTouchBonus(getIntegerBonusFor(ID_TOUCH));
        setHitPointBonus(getIntegerBonusFor(ID_HIT_POINTS));
        setFatiguePointBonus(getIntegerBonusFor(ID_FATIGUE_POINTS));
        mDescription.update();
        setDodgeBonus(getIntegerBonusFor(ID_DODGE_BONUS));
        setParryBonus(getIntegerBonusFor(ID_PARRY_BONUS));
        setBlockBonus(getIntegerBonusFor(ID_BLOCK_BONUS));
        setBasicSpeedBonus(getDoubleBonusFor(ID_BASIC_SPEED));
        setBasicMoveBonus(getIntegerBonusFor(ID_BASIC_MOVE));
        mArmor.update();
        if (!mSkillsUpdated) {
            updateSkills();
        }
        if (!mSpellsUpdated) {
            updateSpells();
        }
        endNotify();
    }

    /**
     * @param id The cost reduction ID to search for.
     * @return The cost reduction, as a percentage.
     */
    public int getCostReductionFor(String id) {
        int           total = 0;
        List<Feature> list  = mFeatureMap.get(id.toLowerCase());

        if (list != null) {
            for (Feature feature : list) {
                if (feature instanceof CostReduction) {
                    total += ((CostReduction) feature).getPercentage();
                }
            }
        }
        if (total > 80) {
            total = 80;
        }
        return total;
    }

    /**
     * @param id The feature ID to search for.
     * @return The bonus.
     */
    public int getIntegerBonusFor(String id) {
        return getIntegerBonusFor(id, null);
    }

    /**
     * @param id      The feature ID to search for.
     * @param toolTip The toolTip being built.
     * @return The bonus.
     */
    public int getIntegerBonusFor(String id, StringBuilder toolTip) {
        int           total = 0;
        List<Feature> list  = mFeatureMap.get(id.toLowerCase());
        if (list != null) {
            for (Feature feature : list) {
                if (feature instanceof Bonus && !(feature instanceof WeaponBonus)) {
                    Bonus bonus = (Bonus) feature;
                    total += bonus.getAmount().getIntegerAdjustedAmount();
                    bonus.addToToolTip(toolTip);
                }
            }
        }
        return total;
    }

    /**
     * @param id                      The feature ID to search for.
     * @param nameQualifier           The name qualifier.
     * @param specializationQualifier The specialization qualifier.
     * @param categoriesQualifier     The categories qualifier.
     * @return The bonuses.
     */
    public List<WeaponBonus> getWeaponComparedBonusesFor(String id, String nameQualifier, String specializationQualifier, Set<String> categoriesQualifier, StringBuilder toolTip) {
        List<WeaponBonus> bonuses = new ArrayList<>();
        int               rsl     = Integer.MIN_VALUE;

        for (Skill skill : getSkillNamed(nameQualifier, specializationQualifier, true, null)) {
            int srsl = skill.getRelativeLevel();

            if (srsl > rsl) {
                rsl = srsl;
            }
        }

        if (rsl != Integer.MIN_VALUE) {
            List<Feature> list = mFeatureMap.get(id.toLowerCase());
            if (list != null) {
                for (Feature feature : list) {
                    if (feature instanceof WeaponBonus) {
                        WeaponBonus bonus = (WeaponBonus) feature;
                        if (bonus.getNameCriteria().matches(nameQualifier) && bonus.getSpecializationCriteria().matches(specializationQualifier) && bonus.getLevelCriteria().matches(rsl) && bonus.matchesCategories(categoriesQualifier)) {
                            bonuses.add(bonus);
                            bonus.addToToolTip(toolTip);
                        }
                    }
                }
            }
        }
        return bonuses;
    }

    /**
     * @param id                      The feature ID to search for.
     * @param nameQualifier           The name qualifier.
     * @param specializationQualifier The specialization qualifier.
     * @param categoryQualifier       The categories qualifier
     * @return The bonus.
     */
    public int getSkillComparedIntegerBonusFor(String id, String nameQualifier, String specializationQualifier, Set<String> categoryQualifier) {
        return getSkillComparedIntegerBonusFor(id, nameQualifier, specializationQualifier, categoryQualifier, null);
    }

    /**
     * @param id                      The feature ID to search for.
     * @param nameQualifier           The name qualifier.
     * @param specializationQualifier The specialization qualifier.
     * @param categoryQualifier       The categories qualifier
     * @param toolTip                 The toolTip being built
     * @return The bonus.
     */
    public int getSkillComparedIntegerBonusFor(String id, String nameQualifier, String specializationQualifier, Set<String> categoryQualifier, StringBuilder toolTip) {
        int           total = 0;
        List<Feature> list  = mFeatureMap.get(id.toLowerCase());
        if (list != null) {
            for (Feature feature : list) {
                if (feature instanceof SkillBonus) {
                    SkillBonus bonus = (SkillBonus) feature;
                    if (bonus.getNameCriteria().matches(nameQualifier) && bonus.getSpecializationCriteria().matches(specializationQualifier) && bonus.matchesCategories(categoryQualifier)) {
                        total += bonus.getAmount().getIntegerAdjustedAmount();
                        bonus.addToToolTip(toolTip);
                    }
                }
            }
        }
        return total;
    }

    /**
     * @param id         The feature ID to search for.
     * @param qualifier  The qualifier.
     * @param categories The categories qualifier
     * @return The bonus.
     */
    public int getSpellComparedIntegerBonusFor(String id, String qualifier, Set<String> categories, StringBuilder toolTip) {
        int           total = 0;
        List<Feature> list  = mFeatureMap.get(id.toLowerCase());
        if (list != null) {
            for (Feature feature : list) {
                if (feature instanceof SpellBonus) {
                    SpellBonus bonus = (SpellBonus) feature;
                    if (bonus.getNameCriteria().matches(qualifier) && bonus.matchesCategories(categories)) {
                        total += bonus.getAmount().getIntegerAdjustedAmount();
                        bonus.addToToolTip(toolTip);
                    }
                }
            }
        }
        return total;
    }

    /**
     * @param id The feature ID to search for.
     * @return The bonus.
     */
    public double getDoubleBonusFor(String id) {
        double        total = 0;
        List<Feature> list  = mFeatureMap.get(id.toLowerCase());
        if (list != null) {
            for (Feature feature : list) {
                if (feature instanceof Bonus && !(feature instanceof WeaponBonus)) {
                    total += ((Bonus) feature).getAmount().getAdjustedAmount();
                }
            }
        }
        return total;
    }

    /**
     * Post an undo edit if we're not currently in an undo.
     *
     * @param name   The name of the undo.
     * @param id     The ID of the field being changed.
     * @param before The original value.
     * @param after  The new value.
     */
    void postUndoEdit(String name, String id, Object before, Object after) {
        StdUndoManager mgr = getUndoManager();
        if (!mgr.isInTransaction()) {
            if (before instanceof ListRow ? !((ListRow) before).isEquivalentTo(after) : !before.equals(after)) {
                addEdit(new CharacterFieldUndo(this, name, id, before, after));
            }
        }
    }
}