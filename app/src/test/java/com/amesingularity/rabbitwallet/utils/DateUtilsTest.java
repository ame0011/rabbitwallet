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

import org.dmfs.rfc5545.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link DateUtils}. The methods under test rely on the default timezone and on
 * the US locale week conventions, so the class pins the default locale to avoid environment
 * dependent expectations (for example {@code Calendar.getFirstDayOfWeek()}).
 */
public class DateUtilsTest {

    private static Locale mPreviousLocale;

    @BeforeClass
    public static void pinLocale() {
        mPreviousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void restoreLocale() {
        Locale.setDefault(mPreviousLocale);
    }

    private static Calendar calendarOf(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Returns the midnight of the given date using the default timezone.
     */
    private static long midnightOf(Date date) {
        Calendar calendar = calendarOf(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////// SQL DATE / DATETIME STRINGS //////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getDateFromSQLDateString() {
        Date date = DateUtils.getDateFromSQLDateString("2018-03-07");
        Calendar calendar = calendarOf(date);
        assertEquals(2018, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH));
        assertEquals(7, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
    }

    @Test
    public void getSQLDateStringIsTheInverseOfGetDateFromSQLDateString() {
        assertEquals("2018-03-07", DateUtils.getSQLDateString(DateUtils.getDateFromSQLDateString("2018-03-07")));
        assertEquals("1999-12-31", DateUtils.getSQLDateString(DateUtils.getDateFromSQLDateString("1999-12-31")));
    }

    @Test
    public void getDateFromSQLDateTimeString() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        Calendar calendar = calendarOf(date);
        assertEquals(2018, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH));
        assertEquals(7, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(23, calendar.get(Calendar.MINUTE));
        assertEquals(45, calendar.get(Calendar.SECOND));
    }

    @Test
    public void getSQLDateTimeStringIsTheInverseOfGetDateFromSQLDateTimeString() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        assertEquals("2018-03-07 14:23:45", DateUtils.getSQLDateTimeString(date));
        assertEquals("2018-03-07 14:23:45", DateUtils.getSQLDateTimeString(date.getTime()));
    }

    @Test
    public void getDateTimeStringWithCustomPattern() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        assertEquals("2018/03/07", DateUtils.getDateTimeString(date, "yyyy/MM/dd"));
    }

    @Test
    public void getFilenameDateTimeString() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        assertEquals("2018-03-07_14-23-45", DateUtils.getFilenameDateTimeString(date));
    }

    @Test
    public void getDateFromSQLDateStringWithInvalidStringThrows() {
        try {
            DateUtils.getDateFromSQLDateString("not-a-date");
            fail("a RuntimeException was expected");
        } catch (RuntimeException e) {
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void getDateFromSQLDateTimeStringWithInvalidStringThrows() {
        try {
            DateUtils.getDateFromSQLDateTimeString("");
            fail("a RuntimeException was expected");
        } catch (RuntimeException e) {
            assertNotNull(e.getCause());
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// CALENDAR BASED HELPERS ///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getDateUsesZeroBasedMonth() {
        Date date = DateUtils.getDate(2018, Calendar.MARCH, 7);
        assertEquals("2018-03-07", DateUtils.getSQLDateString(date));
    }

    @Test
    public void setTimeKeepsTheDayAndChangesTheTime() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        Date updated = DateUtils.setTime(date, 8, 30, 12, 0);
        assertEquals("2018-03-07 08:30:12", DateUtils.getSQLDateTimeString(updated));
    }

    @Test
    public void setTimeOnCalendarKeepsTheDayAndChangesTheTime() {
        Date updated = DateUtils.setTime(calendarOf(DateUtils.getDate(2018, Calendar.MARCH, 7)), 23, 59, 59, 999);
        assertEquals("2018-03-07 23:59:59", DateUtils.getSQLDateTimeString(updated));
    }

    @Test
    public void setDayOfMonth() {
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        Date updated = DateUtils.setDayOfMonth(calendar, 21);
        assertEquals("2018-03-21", DateUtils.getSQLDateString(updated));
    }

    @Test
    public void setMonth() {
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        Date updated = DateUtils.setMonth(calendar, Calendar.NOVEMBER);
        assertEquals("2018-11-07", DateUtils.getSQLDateString(updated));
    }

    @Test
    public void setDayAndMonth() {
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        Date updated = DateUtils.setDayAndMonth(calendar, 25, Calendar.DECEMBER);
        assertEquals("2018-12-25", DateUtils.getSQLDateString(updated));
    }

    @Test
    public void addDays() {
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        assertEquals("2018-03-14", DateUtils.getSQLDateString(DateUtils.addDays(calendar, 7)));
        assertEquals("2018-02-28", DateUtils.getSQLDateString(DateUtils.addDays(calendar, -14)));
    }

    @Test
    public void addMonths() {
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        assertEquals("2018-07-07", DateUtils.getSQLDateString(DateUtils.addMonths(calendar, 4)));
        assertEquals("2017-11-07", DateUtils.getSQLDateString(DateUtils.addMonths(calendar, -8)));
    }

    @Test
    public void addMonthAndDay() {
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        assertEquals("2018-04-10", DateUtils.getSQLDateString(DateUtils.addMonthAndDay(calendar, 1, 3)));
    }

    @Test
    public void getFirstDateOfWeekWithMondayAsFirstDay() {
        // 2018-03-07 is a Wednesday, the monday of the same week is 2018-03-05
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        assertEquals("2018-03-05",
                DateUtils.getSQLDateString(DateUtils.getFirstDateOfWeek(calendar, Calendar.MONDAY)));
    }

    @Test
    public void getFirstDateOfWeekWithSundayAsFirstDay() {
        // 2018-03-07 is a Wednesday, the sunday of the same week is 2018-03-04
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        assertEquals("2018-03-04",
                DateUtils.getSQLDateString(DateUtils.getFirstDateOfWeek(calendar, Calendar.SUNDAY)));
    }

    @Test
    public void getFirstDateOfWeekMovesToPreviousWeekWhenTheDayPrecedesTheFirstDayOfWeek() {
        // with the US conventions (week starting on sunday) the monday of the week containing
        // sunday 2018-03-04 is 2018-02-26
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 4));
        assertEquals("2018-02-26",
                DateUtils.getSQLDateString(DateUtils.getFirstDateOfWeek(calendar, Calendar.MONDAY)));
    }

    @Test
    public void getFirstDateOfWeekUsesTheLocaleFirstDayOfWeek() {
        // the default locale is pinned to US, so the first day of the week is sunday
        Calendar calendar = DateUtils.getCalendar(DateUtils.getDate(2018, Calendar.MARCH, 7));
        assertEquals("2018-03-04", DateUtils.getSQLDateString(DateUtils.getFirstDateOfWeek(calendar)));
    }

    @Test
    public void getCalendarReturnsACalendarSetOnTheGivenDate() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        Calendar calendar = DateUtils.getCalendar(date);
        assertEquals(date, calendar.getTime());
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// COMPARISON HELPERS ///////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void isBeforeToday() {
        long oneDay = 24L * 60 * 60 * 1000;
        long todayMidnight = midnightOf(new Date());
        assertTrue(DateUtils.isBeforeToday(new Date(todayMidnight - oneDay)));
        assertFalse(DateUtils.isBeforeToday(new Date(todayMidnight)));
        assertFalse(DateUtils.isBeforeToday(new Date(todayMidnight + oneDay)));
    }

    @Test
    public void isBeforeNow() {
        assertTrue(DateUtils.isBeforeNow(new Date(System.currentTimeMillis() - 60_000)));
        assertFalse(DateUtils.isBeforeNow(new Date(System.currentTimeMillis() + 60_000)));
    }

    @Test
    public void isAfterAndIsAfterEqual() {
        Date older = DateUtils.getDateFromSQLDateString("2018-03-07");
        Date newer = DateUtils.getDateFromSQLDateString("2018-03-08");
        assertTrue(DateUtils.isAfter(newer, older));
        assertFalse(DateUtils.isAfter(older, newer));
        assertFalse(DateUtils.isAfter(newer, newer));
        // equal dates are accepted by the "or equal" flavor
        assertTrue(DateUtils.isAfterEqual(newer, older));
        assertTrue(DateUtils.isAfterEqual(newer, newer));
        assertFalse(DateUtils.isAfterEqual(older, newer));
    }

    @Test
    public void isBeforeAndIsBeforeEqual() {
        Date older = DateUtils.getDateFromSQLDateString("2018-03-07");
        Date newer = DateUtils.getDateFromSQLDateString("2018-03-08");
        assertTrue(DateUtils.isBefore(older, newer));
        assertFalse(DateUtils.isBefore(newer, older));
        assertTrue(DateUtils.isBeforeEqual(older, older));
        assertFalse(DateUtils.isBeforeEqual(newer, older));
    }

    @Test
    public void isTheSameDay() {
        Date morning = DateUtils.getDateFromSQLDateTimeString("2018-03-07 08:00:00");
        Date evening = DateUtils.getDateFromSQLDateTimeString("2018-03-07 20:00:00");
        Date nextDay = DateUtils.getDateFromSQLDateTimeString("2018-03-08 08:00:00");
        assertTrue(DateUtils.isTheSameDay(morning, evening));
        assertFalse(DateUtils.isTheSameDay(morning, nextDay));
    }

    @Test
    public void isTheSameDayAndHour() {
        Date at810 = DateUtils.getDateFromSQLDateTimeString("2018-03-07 08:10:00");
        Date at850 = DateUtils.getDateFromSQLDateTimeString("2018-03-07 08:50:00");
        Date at910 = DateUtils.getDateFromSQLDateTimeString("2018-03-07 09:10:00");
        assertTrue(DateUtils.isTheSameDayAndHour(at810, at850));
        assertFalse(DateUtils.isTheSameDayAndHour(at810, at910));
    }

    @Test
    public void getDaysBetween() {
        Date start = DateUtils.getDateFromSQLDateString("2018-01-01");
        Date end = DateUtils.getDateFromSQLDateString("2018-01-04");
        assertEquals(3, DateUtils.getDaysBetween(start, end));
        // the order of the arguments does not matter
        assertEquals(3, DateUtils.getDaysBetween(end, start));
        assertEquals(0, DateUtils.getDaysBetween(start, start));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////// RFC 5545 DATE TIME CONVERSIONS ////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getFixedDateTimeDropsTheTime() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        DateTime dateTime = DateUtils.getFixedDateTime(date);
        assertEquals(2018, dateTime.getYear());
        assertEquals(Calendar.MARCH, dateTime.getMonth());
        assertEquals(7, dateTime.getDayOfMonth());
    }

    @Test
    public void getFixedDateConvertsBackToAMidnightDate() {
        Date date = DateUtils.getFixedDate(new DateTime(2018, Calendar.MARCH, 7));
        assertEquals("2018-03-07", DateUtils.getSQLDateString(date));
        Calendar calendar = calendarOf(date);
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void fixedDateTimeRoundTrip() {
        Date date = DateUtils.getDateFromSQLDateTimeString("2018-03-07 14:23:45");
        Date fixed = DateUtils.getFixedDate(DateUtils.getFixedDateTime(date));
        assertEquals(DateUtils.getDateFromSQLDateString("2018-03-07"), fixed);
    }
}
