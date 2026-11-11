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

package com.amesingularity.rabbitwallet.utils;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import androidx.test.core.app.ApplicationProvider;

import com.amesingularity.rabbitwallet.TestApplication;
import com.amesingularity.rabbitwallet.model.CurrencyUnit;
import com.amesingularity.rabbitwallet.model.Money;
import com.amesingularity.rabbitwallet.storage.preference.PreferenceManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MoneyFormatter}. The tests run on Robolectric because the formatter
 * reads the income/expense colors from {@link PreferenceManager} and the primary text color
 * from {@code ThemeEngine}. Number formatting expectations assume the US locale, which is
 * pinned for the whole class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class MoneyFormatterTest {

    private static final CurrencyUnit EUR = new CurrencyUnit("EUR", "Euro", "€", 2);
    private static final CurrencyUnit JPY = new CurrencyUnit("JPY", "Japanese Yen", "¥", 0);
    private static final CurrencyUnit BHD = new CurrencyUnit("BHD", "Bahraini Dinar", "BD", 3);

    private static Locale mPreviousLocale;

    private MoneyFormatter mFormatter;

    @BeforeClass
    public static void pinLocale() {
        mPreviousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void restoreLocale() {
        Locale.setDefault(mPreviousLocale);
    }

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        // the money overload formats the currencies through the currency manager
        CurrencyManager.initialize(context);
        mFormatter = MoneyFormatter.getInstance();
        // start from a fully known state regardless of the preference defaults
        mFormatter.setCurrencyEnabled(false);
        mFormatter.setGroupDigitEnabled(true);
        mFormatter.setRoundDecimalsEnabled(false);
        mFormatter.setShowSymbolEnabled(false);
    }

    @After
    public void tearDown() {
        mFormatter = null;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// NORMALIZE ////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void normalizeAppliesTheDecimalOffset() {
        assertEquals(123000L, MoneyFormatter.normalize(123L, 3));
        assertEquals(123L, MoneyFormatter.normalize(123L, 0));
        // shrinking the decimals truncates towards zero (legacy behavior)
        assertEquals(1L, MoneyFormatter.normalize(123L, -2));
        assertEquals(-12L, MoneyFormatter.normalize(-12345L, -3));
    }

    @Test
    public void normalizeConvertsBetweenTwoDecimalCounts() {
        assertEquals(12345000L, MoneyFormatter.normalize(12345L, 2, 5));
        assertEquals(12345L, MoneyFormatter.normalize(12345L, 2, 2));
        assertEquals(12L, MoneyFormatter.normalize(12345L, 5, 2));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////// NOT TINTED SINGLE CURRENCY ///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void positiveAmountWithDefaultFlags() {
        assertEquals("1,234.56", mFormatter.getNotTintedString(EUR, 123456L));
    }

    @Test
    public void negativeAmountCarriesTheMinusSign() {
        assertEquals("-1,234.56", mFormatter.getNotTintedString(EUR, -123456L));
    }

    @Test
    public void zeroAmountIsFormattedWithTheCurrencyDecimals() {
        assertEquals("0.00", mFormatter.getNotTintedString(EUR, 0L));
        assertEquals("0.000", mFormatter.getNotTintedString(BHD, 0L));
        assertEquals("0", mFormatter.getNotTintedString(JPY, 0L));
    }

    @Test
    public void decimalsOfTheCurrencyDriveTheFractionDigits() {
        // 1.234 with a 3 decimals currency, 1234 with a 0 decimals one
        assertEquals("1.234", mFormatter.getNotTintedString(BHD, 1234L));
        assertEquals("1,234", mFormatter.getNotTintedString(JPY, 1234L));
    }

    @Test
    public void currencySymbolIsShownOnlyWhenRequested() {
        assertEquals("1,234.56", mFormatter.getNotTintedString(EUR, 123456L, MoneyFormatter.CurrencyMode.USER_PREFERENCE));
        assertEquals("€ 1,234.56", mFormatter.getNotTintedString(EUR, 123456L, MoneyFormatter.CurrencyMode.ALWAYS_SHOWN));
        // ALWAYS_HIDDEN behaves like USER_PREFERENCE with the currency disabled
        assertEquals("1,234.56", mFormatter.getNotTintedString(EUR, 123456L, MoneyFormatter.CurrencyMode.ALWAYS_HIDDEN));
        // with the preference enabled the symbol appears in USER_PREFERENCE mode
        mFormatter.setCurrencyEnabled(true);
        assertEquals("€ 1,234.56", mFormatter.getNotTintedString(EUR, 123456L, MoneyFormatter.CurrencyMode.USER_PREFERENCE));
    }

    @Test
    public void negativeAmountWithCurrencySymbolKeepsTheSignAfterTheSymbol() {
        assertEquals("€ -1,234.56", mFormatter.getNotTintedString(EUR, -123456L, MoneyFormatter.CurrencyMode.ALWAYS_SHOWN));
    }

    @Test
    public void groupDigitsCanBeDisabled() {
        mFormatter.setGroupDigitEnabled(false);
        assertEquals("1234.56", mFormatter.getNotTintedString(EUR, 123456L));
    }

    @Test
    public void roundDecimalsRoundsHalfUpOnTheAbsoluteValue() {
        mFormatter.setRoundDecimalsEnabled(true);
        assertEquals("1,235", mFormatter.getNotTintedString(EUR, 123456L));
        // 1234.49 -> 1234, the sign is applied before rounding the absolute value
        assertEquals("-1,234", mFormatter.getNotTintedString(EUR, -123449L));
    }

    @Test
    public void plusSymbolIsShownForNonNegativeAmountsWhenRequested() {
        mFormatter.setShowSymbolEnabled(true);
        assertEquals("+1,234.56", mFormatter.getNotTintedString(EUR, 123456L));
        assertEquals("+0.00", mFormatter.getNotTintedString(EUR, 0L));
        // negative amounts never get the plus sign
        assertEquals("-1,234.56", mFormatter.getNotTintedString(EUR, -123456L));
    }

    @Test
    public void nullCurrencyFallsBackToTwoDecimalsAndNoSymbol() {
        assertEquals("123.45", mFormatter.getNotTintedString(null, 12345L));
        // even with the symbol requested there is nothing to show for an unknown currency
        mFormatter.setCurrencyEnabled(true);
        assertEquals("123.45", mFormatter.getNotTintedString(null, 12345L, MoneyFormatter.CurrencyMode.ALWAYS_SHOWN));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// NOT TINTED MONEY /////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void emptyMoneyIsRenderedWithThePlaceholder() {
        assertEquals(" --- ", mFormatter.getNotTintedString(Money.empty()));
    }

    @Test
    public void moneyWithASingleCurrencyFollowsTheUserPreference() {
        assertNotNull("EUR currency should be loaded from the assets", CurrencyManager.getCurrency("EUR"));
        Money money = new Money("EUR", 123456L);
        assertEquals("1,234.56", mFormatter.getNotTintedString(money));
        mFormatter.setCurrencyEnabled(true);
        assertEquals("€ 1,234.56", mFormatter.getNotTintedString(money));
    }

    @Test
    public void moneyWithMultipleCurrenciesAlwaysShowsTheSymbols() {
        assertNotNull("EUR currency should be loaded from the assets", CurrencyManager.getCurrency("EUR"));
        assertNotNull("USD currency should be loaded from the assets", CurrencyManager.getCurrency("USD"));
        Money money = new Money("EUR", 123456L);
        money.addMoney("USD", 500000L);
        // the iteration order of the internal map is unspecified, check both orders
        String formatted = mFormatter.getNotTintedString(money);
        String first = "€ 1,234.56 - $ 5,000.00";
        String second = "$ 5,000.00 - € 1,234.56";
        assertTrue("unexpected format: " + formatted, formatted.equals(first) || formatted.equals(second));
    }

    @Test
    public void moneyAggregatesAmountOfTheSameCurrency() {
        Money money = new Money("EUR", 100000L);
        money.addMoney("EUR", 23456L);
        assertEquals("1,234.56", mFormatter.getNotTintedString(money));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// TINTED STRINGS ///////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void tintedStringOfPositiveAmountUsesTheIncomeColor() {
        SpannableString tinted = mFormatter.getTintedString(EUR, 123456L);
        assertEquals("1,234.56", tinted.toString());
        ForegroundColorSpan[] spans = tinted.getSpans(0, tinted.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(PreferenceManager.getCurrentIncomeColor(), spans[0].getForegroundColor());
    }

    @Test
    public void tintedStringOfNegativeAmountUsesTheExpenseColor() {
        // with the plus/minus symbol disabled the sign is hidden and the color carries the flow
        SpannableString tinted = mFormatter.getTintedString(EUR, -123456L);
        assertEquals("1,234.56", tinted.toString());
        ForegroundColorSpan[] spans = tinted.getSpans(0, tinted.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(PreferenceManager.getCurrentExpenseColor(), spans[0].getForegroundColor());
    }

    @Test
    public void tintedNegativeAmountKeepsTheSignWhenSymbolsAreEnabled() {
        mFormatter.setShowSymbolEnabled(true);
        SpannableString tinted = mFormatter.getTintedString(EUR, -123456L);
        assertEquals("-1,234.56", tinted.toString());
        ForegroundColorSpan[] spans = tinted.getSpans(0, tinted.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(PreferenceManager.getCurrentExpenseColor(), spans[0].getForegroundColor());
    }

    @Test
    public void explicitIncomeTintForcesThePlusFlowOnNegativeAmounts() {
        // with the plus/minus symbol disabled a forced income flow is always rendered positive
        Money money = new Money("EUR", -123456L);
        SpannableStringBuilder builder = mFormatter.getTintedString(money, MoneyFormatter.TintMode.INCOME);
        String text = builder.toString();
        assertEquals("1,234.56", text);
        ForegroundColorSpan[] spans = builder.getSpans(0, builder.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(PreferenceManager.getCurrentIncomeColor(), spans[0].getForegroundColor());
    }

    @Test
    public void explicitExpenseTintUsesTheExpenseColor() {
        Money money = new Money("EUR", 123456L);
        SpannableStringBuilder builder = mFormatter.getTintedString(money, MoneyFormatter.TintMode.EXPENSE);
        assertEquals("1,234.56", builder.toString());
        ForegroundColorSpan[] spans = builder.getSpans(0, builder.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
        assertEquals(PreferenceManager.getCurrentExpenseColor(), spans[0].getForegroundColor());
    }

    @Test
    public void tintedEmptyMoneyUsesTheNeutralColorPlaceholder() {
        SpannableStringBuilder builder = mFormatter.getTintedString(Money.empty());
        assertEquals(" --- ", builder.toString());
        ForegroundColorSpan[] spans = builder.getSpans(0, builder.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length);
    }

    @Test
    public void tintedMultipleCurrenciesAreJoinedWithANeutralDivider() {
        assertNotNull("EUR currency should be loaded from the assets", CurrencyManager.getCurrency("EUR"));
        assertNotNull("USD currency should be loaded from the assets", CurrencyManager.getCurrency("USD"));
        Money money = new Money("EUR", 123456L);
        money.addMoney("USD", 500000L);
        SpannableStringBuilder builder = mFormatter.getTintedString(money);
        String text = builder.toString();
        assertTrue("unexpected format: " + text,
                text.equals("€ 1,234.56 - $ 5,000.00") || text.equals("$ 5,000.00 - € 1,234.56"));
        // one span per currency plus one for the divider
        ForegroundColorSpan[] spans = builder.getSpans(0, builder.length(), ForegroundColorSpan.class);
        assertEquals(3, spans.length);
    }
}
