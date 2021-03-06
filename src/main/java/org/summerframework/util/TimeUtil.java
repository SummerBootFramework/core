/*
 * Copyright 2005 The Summer Boot Framework Project
 *
 * The Summer Boot Framework Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.summerframework.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Changski Tie Zheng Zhang, Du Xiao
 */
public class TimeUtil {

    public static long getSecondsSinceMidnight(Calendar c) {
        return 3600 * c.get(Calendar.HOUR_OF_DAY) + 60 * c.get(Calendar.MINUTE) + c.get(Calendar.SECOND);
    }

    public static long getSecondsTillMidnight(Calendar c) {
        return 86400 - getSecondsSinceMidnight(c);
    }

    public static int[] seconds2DHMS(long totalSeconds) {
        int[] ymdhms = {0, 0, 0, 0};
        long day = totalSeconds / 86400;
        long hour = (totalSeconds / 3600) % 24;
        long minute = (totalSeconds % 3600) / 60;
        long second = totalSeconds % 60;
        ymdhms[3] = (int) day;
        ymdhms[2] = (int) hour;
        ymdhms[1] = (int) minute;
        ymdhms[0] = (int) second;

        return ymdhms;
    }

    public static String seconds2DHMSString(long totalSeconds) {
        int[] ymdhms = seconds2DHMS(totalSeconds);
        int day = ymdhms[3];
        int hour = ymdhms[2];
        int min = ymdhms[1];
        int sec = ymdhms[0];
        return new StringBuilder()
                .append(day).append(" day").append(day > 1 ? "s " : " ")
                .append(hour).append(" hour").append(hour > 1 ? "s " : " ")
                .append(min).append(" min").append(min > 1 ? "s " : " ")
                .append(sec).append(" sec").append(sec > 1 ? "s " : " ")
                .toString();

    }

    public static DateTimeFormatter UTC_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static ZoneId ZONE_ID_ONTARIO = ZoneId.of("America/Toronto");

    /**
     * Maps the UTC time to an ET format.
     *
     * @param utcTime UTC time to be formatted.
     * @param zoneId
     *
     * @return ET formatted time.
     *
     */
    public static String utcDateTimeToLocalDateTime(String utcTime, ZoneId zoneId) {
        if (StringUtils.isBlank(utcTime)) {
            return null;
        }
        return ZonedDateTime.parse(utcTime, UTC_DATE_TIME_FORMATTER)
                .withZoneSameInstant(zoneId)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    }

    public static LocalDateTime toLocalDateTime(long utcTs) {
        return toLocalDateTime(utcTs, ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(long utcTs, ZoneId zoneId) {
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }
        return Instant.ofEpochMilli(utcTs).atZone(zoneId).toLocalDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(long utcTs, ZoneId zoneId) {
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }
        return Instant.ofEpochMilli(utcTs).atZone(zoneId).toOffsetDateTime();
    }

    protected static Random RANDOM = new Random();

    public static int random(int low, int high) {
        int result = RANDOM.nextInt(high - low) + low;
        return result;
    }
}
