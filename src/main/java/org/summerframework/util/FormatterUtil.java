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

import java.awt.image.BufferedImage;
import static org.summerframework.boot.config.ConfigUtil.DECRYPTED_WARPER_PREFIX;
import static org.summerframework.boot.config.ConfigUtil.ENCRYPTED_WARPER_PREFIX;
import org.summerframework.security.SecurityUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Changski Tie Zheng Zhang, Du Xiao
 */
public class FormatterUtil {

    public static final long INT_MASK = 0xFFFFFFFFL;//(long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
    public static final int SHORT_MASK = 0xFFFF;
    public static final short BYTE_MASK = 0xFF;
    public static final short NIBBLE_MASK = 0x0F;

    public static final String[] EMPTY_STR_ARRAY = {};
    public static final String REGEX_CSV = "\\s*,\\s*";
    public static final String REGEX_URL = "\\s*/\\s*";
    public static final String REGEX_BINDING_MAP = "\\s*:\\s*";
    public static final String REGEX_EMAIL = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    public static final Pattern REGEX_EMAIL_PATTERN = Pattern.compile(REGEX_EMAIL);

    public static String[] parseDsv(String csv, String delimiter) {
        return StringUtils.isBlank(csv) ? EMPTY_STR_ARRAY : csv.trim().split("\\s*" + delimiter + "\\s*");
    }

    public static String[] parseCsv(String csv) {
        return StringUtils.isBlank(csv) ? EMPTY_STR_ARRAY : csv.trim().split(REGEX_CSV);
    }

    public static String[] parseURL(String url) {
        return StringUtils.isBlank(url) ? EMPTY_STR_ARRAY : url.trim().split(REGEX_URL);
    }

    public static String[] parseURL(String url, boolean trim) {
        return StringUtils.isBlank(url)
                ? EMPTY_STR_ARRAY
                : trim ? url.trim().split(REGEX_URL) : url.split(REGEX_URL);
    }

    public static <T extends Object> String toCSV(Collection<T> a) {
        return a.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static String[] getEnumNames(Class<? extends Enum<?>> e) {
        //return Arrays.stream(MyEnum.values()).map(MyEnum::name).toArray(String[]::new);
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    public static Pattern INSIDE_PARENTHESES_VALUE = Pattern.compile("\\(([^)]+)\\)");

    public static String getInsideParenthesesValue(String value) {
        String ret = value;
//        Matcher m = INSIDE_PARENTHESES_VALUE.matcher(value);
//        if (m.find()) {
//                ret = m.group(1);
//        }
//        return ret;
        int end = value.lastIndexOf(")");
        if (end >= 0) {
            ret = value.substring(value.indexOf("(") + 1, end);
        }
        return ret;
    }

    public static String scrapeJson(String target, String plain) {
        if (plain == null) {
            return null;
        }
        return plain.replaceAll("\"" + target + "\"\\s*:\\s*\"\\s*[0-9]*", "\"" + target + "\":\"********");
    }

    private static final Pattern REGEX_DEC_PATTERN = Pattern.compile(DECRYPTED_WARPER_PREFIX + "\\(([^)]+)\\)");
    private static final Pattern REGEX_ENC_PATTERN = Pattern.compile(ENCRYPTED_WARPER_PREFIX + "\\(([^)]+)\\)");

    public static String updateProtectedLine(String line, boolean encrypt) throws GeneralSecurityException {
        Matcher matcher = encrypt
                ? REGEX_DEC_PATTERN.matcher(line)
                : REGEX_ENC_PATTERN.matcher(line);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        if (matches.isEmpty()) {
            return null;
        }
        for (String match : matches) {
            //try {
                String converted;
                if (encrypt) {
                    converted = SecurityUtil.encrypt(match, true);
                    line = line.replace(DECRYPTED_WARPER_PREFIX + "(" + match + ")", ENCRYPTED_WARPER_PREFIX + "(" + converted + ")");
                } else {
                    converted = SecurityUtil.decrypt(match, true);
                    line = line.replace(ENCRYPTED_WARPER_PREFIX + "(" + match + ")", DECRYPTED_WARPER_PREFIX + "(" + converted + ")");
                }
            //} catch (Throwable ex) {
            //    System.err.println(ex + " - " + match + ": " + line);
            //}
        }
        return line;
    }

    public static String b2n(String s) {
        return StringUtils.isBlank(s) ? null : s.trim();
    }

    /*
    * BindAddresses = 192.168.1.10:8445, 127.0.0.1:8446, 0.0.0.0:8447
     */
    public static Map<String, Integer> parseBindingAddresss(String bindAddresses) {
        //int[] ports = Arrays.stream(portsStr).mapToInt(Integer::parseInt).toArray();
        Map<String, Integer> ret = new HashMap<>();
        String[] addrs = parseCsv(bindAddresses);
        for (String addr : addrs) {
            String[] ap = addr.trim().split(REGEX_BINDING_MAP);
            ret.put(ap[0], Integer.parseInt(ap[1]));
        }
        return ret;
    }

    public static Map<String, String> parseMap(String mapCVS) {
        //int[] ports = Arrays.stream(portsStr).mapToInt(Integer::parseInt).toArray();
        Map<String, String> ret = new HashMap<>();
        String[] mapKeyValues = parseCsv(mapCVS);
        for (String mapKeyValue : mapKeyValues) {
            String[] ap = mapKeyValue.trim().split(REGEX_BINDING_MAP);
            ret.put(ap[0], ap[1]);
        }
        return ret;
    }

    public static String convertTo(String value, String targetCharsetName) throws UnsupportedEncodingException {
        //String rawString = "Fantastic???????? Entwickeln Sie mit Vergn??gen";
        return new String(value.getBytes(targetCharsetName), targetCharsetName);
    }

    public static String base64MimeEncode(byte[] contentBytes) {
        return Base64.getMimeEncoder().encodeToString(contentBytes);
    }

    public static byte[] base64MimeDecode(String encodedMime) {
        return Base64.getMimeDecoder().decode(encodedMime);
    }

    public static String base64Encode(byte[] contentBytes) {
        return Base64.getEncoder().encodeToString(contentBytes);
    }

    public static byte[] base64Decode(String encodedMime) {
        return Base64.getDecoder().decode(encodedMime);
    }

    /**
     *
     * @param bi
     * @param format - png
     * @return
     * @throws IOException ImageIO failed to access system cache dir
     */
    public static byte[] toByteArray(BufferedImage bi, String format) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            ImageIO.write(bi, format, baos);
            byte[] bytes = baos.toByteArray();
            return bytes;
        }
    }

    public static String toString(ByteBuffer buffer) {
        return toString(buffer, true, true, 8, "    ");
    }

    public static String toString(ByteBuffer buffer, boolean showStatus, boolean showHeaderfooter, int showNumberOfBytesPerLine, String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (showStatus) {
            sb.append("ByteBuffer status:")
                    .append(" Order=").append(buffer.order())
                    .append(" Position=").append(buffer.position())
                    .append(" Limit=").append(buffer.limit())
                    .append(" Capacity=").append(buffer.capacity())
                    .append(" Remaining=").append(buffer.remaining());
        }
        if (showHeaderfooter) {
            sb.append("\n************** ByteBuffer Contents starts **************\n");
        }
        boolean eol = false;
        if (showNumberOfBytesPerLine > 0) {
            byte[] array = buffer.array();
            for (int i = 0; i < buffer.limit(); i++) {
                eol = (i + 1) % showNumberOfBytesPerLine == 0;
                sb.append(String.format("0x%02X", array[i])).append(eol ? "\n" : delimiter);
            }
        }
        if (showHeaderfooter) {
            if (!eol) {
                sb.append("\n");
            }
            sb.append("************** ByteBuffer Contents ends **************\n");
        }
        return sb.toString();
    }

    /**
     * For old Java before Java 17 HexFormat.of().parseHex(s)
     *
     * @param hexString must be an even-length string
     * @return
     */
    public static byte[] parseHex(String hexString) {
        String evenLengthHexString = hexString.replaceAll("0x", "").replaceAll("[^a-zA-Z0-9]", "");//remove "0x" and other delimiters, 0x5A   ,;/n/t|...   0x01 -> 5A01
        int len = evenLengthHexString.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Converted Hex string length=" + len + " and is not an even-length: \n\t arg: " + hexString + "\n\t hex: " + evenLengthHexString);
        }
        String hex = evenLengthHexString.replaceAll("[^a-fA-F0-9]", "");
        if (!evenLengthHexString.equals(hex)) {
            throw new IllegalArgumentException("Invalid Hex string \n\t arg: " + hexString + "\n\t hex: " + evenLengthHexString);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(evenLengthHexString.charAt(i), 16) << 4)
                    + Character.digit(evenLengthHexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static <T extends Object> Set<T> findDuplicates(List<T> listContainingDuplicates) {
        final Set<T> setToReturn = new HashSet<>();
        final Set<T> set1 = new HashSet<>();

        for (T yourInt : listContainingDuplicates) {
            if (!set1.add(yourInt)) {
                setToReturn.add(yourInt);
            }
        }
        return setToReturn;
    }
}
