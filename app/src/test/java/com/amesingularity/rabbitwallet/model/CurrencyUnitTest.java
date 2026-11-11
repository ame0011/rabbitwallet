/*
 * Copyright (c) 2026.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amesingularity.rabbitwallet.model;

import android.os.Parcel;

import com.amesingularity.rabbitwallet.TestApplication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CurrencyUnit}. The test runs on Robolectric because the class implements
 * {@link android.os.Parcelable} and the parcel round trip needs the framework implementation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class CurrencyUnitTest {

    private static CurrencyUnit sampleUnit() {
        return new CurrencyUnit("EUR", "Euro", "€", 2);
    }

    @Test
    public void gettersReturnTheConstructorValues() {
        CurrencyUnit euro = sampleUnit();
        assertEquals("EUR", euro.getIso());
        assertEquals("Euro", euro.getName());
        assertEquals("€", euro.getSymbol());
        assertEquals(2, euro.getDecimals());
    }

    @Test
    public void hasDecimalsIsTrueOnlyWhenDecimalsAreAvailable() {
        assertTrue(new CurrencyUnit("EUR", "Euro", "€", 2).hasDecimals());
        assertTrue(new CurrencyUnit("JOD", "Jordanian Dinar", "J.D.", 3).hasDecimals());
        assertFalse(new CurrencyUnit("JPY", "Japanese Yen", "¥", 0).hasDecimals());
    }

    @Test
    public void describeContentsIsZero() {
        assertEquals(0, sampleUnit().describeContents());
    }

    @Test
    public void equalsIsBasedOnTheIsoCode() {
        CurrencyUnit euro = sampleUnit();
        assertEquals(euro, euro);
        // a currency unit built from a different source (e.g. the database) is the same currency
        assertEquals(euro, new CurrencyUnit("EUR", "Euro changed", "$", 3));
        assertNotEquals(euro, new CurrencyUnit("USD", "Euro", "€", 2));
        assertNotEquals(euro, null);
        assertNotEquals(euro, "EUR");
    }

    @Test
    public void writeToParcelCanBeReadBackFromTheCreator() {
        CurrencyUnit original = sampleUnit();
        Parcel parcel = Parcel.obtain();
        try {
            original.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            CurrencyUnit restored = CurrencyUnit.CREATOR.createFromParcel(parcel);
            assertNotNull(restored);
            assertEquals(original, restored);
            assertEquals(original.getIso(), restored.getIso());
            assertEquals(original.getName(), restored.getName());
            assertEquals(original.getSymbol(), restored.getSymbol());
            assertEquals(original.getDecimals(), restored.getDecimals());
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void creatorNewArrayCreatesTheRequestedSize() {
        assertEquals(0, CurrencyUnit.CREATOR.newArray(0).length);
        assertEquals(3, CurrencyUnit.CREATOR.newArray(3).length);
    }
}
