/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 31.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package com.junrar.unpack.vm;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public enum VMStandardFilters {
    VMSF_NONE(0),
    VMSF_E8(1),
    VMSF_E8E9(2),
    VMSF_ITANIUM(3),
    VMSF_RGB(4),
    VMSF_AUDIO(5),
    VMSF_DELTA(6),
    VMSF_UPCASE(7);

    private final int filter;

    VMStandardFilters(int filter) {
        this.filter = filter;
    }

    public int getFilter() {
        return filter;
    }

    public boolean equals(int filter) {
        return this.filter == filter;
    }

    public static VMStandardFilters findFilter(int filter) {
        if (VMSF_NONE.equals(filter)) {
            return VMSF_NONE;
        }

        if (VMSF_E8.equals(filter)) {
            return VMSF_E8;
        }

        if (VMSF_E8E9.equals(filter)) {
            return VMSF_E8E9;
        }
        if (VMSF_ITANIUM.equals(filter)) {
            return VMSF_ITANIUM;
        }

        if (VMSF_RGB.equals(filter)) {
            return VMSF_RGB;
        }

        if (VMSF_AUDIO.equals(filter)) {
            return VMSF_AUDIO;
        }
        if (VMSF_DELTA.equals(filter)) {
            return VMSF_DELTA;
        }
        if (VMSF_UPCASE.equals(filter)) {
            return VMSF_UPCASE;
        }
        return null;
    }

}
