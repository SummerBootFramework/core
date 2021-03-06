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
package org.summerframework.nio.server.ws.rs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Changski Tie Zheng Zhang, Du Xiao
 */
class MetaMatrixParam {

    //"/services/test/v1;a=123;b=;c=345/aaa/{pa1}/bbb/2020;color=red";
    public static final String MATRIX_REGEX_POSTFIX = "\\s*=\\s*(.*?)[;|/]";// works for a, b, c, not work for color
    public static final String MATRIX_REGEX_POSTFIX_END = "\\s*=\\s*(.*?)$"; //works for color, not work for a, b, c, 

    private final String key;
    private final Pattern matrixParamPattern;
    private final Pattern matrixParamPatternEnd;

    public MetaMatrixParam(String key) {
        this.key = key;
        matrixParamPattern = Pattern.compile(key + MATRIX_REGEX_POSTFIX);
        matrixParamPatternEnd = Pattern.compile(key + MATRIX_REGEX_POSTFIX_END);
    }

    public String value(String path) {
        String v = null;
        Matcher fm = matrixParamPattern.matcher(path);
        if (fm.find()) {
            v = fm.group(1);
        } else {
            fm = matrixParamPatternEnd.matcher(path);
            if (fm.find()) {
                v = fm.group(1);
            }
        }
        return v == null ? null : v.trim();
    }

    public String getKey() {
        return key;
    }

}
