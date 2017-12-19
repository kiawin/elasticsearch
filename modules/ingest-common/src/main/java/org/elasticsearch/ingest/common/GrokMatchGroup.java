/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.ingest.common;

final class GrokMatchGroup {
    private static final String DEFAULT_TYPE = "string";
    private final String patternName;
    private final String fieldName;
    private final String type;
    private final String groupValue;

    GrokMatchGroup(String groupName, String groupValue) {
        String[] parts = groupName.split(":");
        patternName = parts[0];
        if (parts.length >= 2) {
            fieldName = parts[1];
        } else {
            fieldName = null;
        }

        if (parts.length == 3) {
            type = parts[2];
        } else {
            type = DEFAULT_TYPE;
        }
        this.groupValue = groupValue;
    }

    public String getName() {
        return (fieldName == null) ? patternName : fieldName;
    }

    public Object getValue() {
        /**
         * raw log may return "-" value for number-type field.
         */
        if (groupValue == null || "-".equals(groupValue) || "".equals(groupValue)) { return null; }

        String[] values = getSeparatedValues(groupValue);
        if (isSeparatedValuesEmpty(values)) {
            return null;
        }

        switch(type) {
            case "int":
                int returnIntValue = 0;
                for (String v : values) {
                    if (isNotValueEmpty(v)) {
                        returnIntValue += Integer.parseInt(v.trim());
                    }
                }
                return returnIntValue;
            case "float":
                float returnFloatValue = 0;
                for (String v : values) {
                    if (isNotValueEmpty(v)) {
                        returnFloatValue += Float.parseFloat(v.trim());
                    }
                }
                return returnFloatValue;
            case "long":
                long returnLongValue = 0;
                for (String v : values) {
                    if (isNotValueEmpty(v)) {
                        returnLongValue += Long.parseLong(v.trim());
                    }
                }
                return returnLongValue;
            default:
                return groupValue;
        }
    }

    private String[] getSeparatedValues(String groupValue) {
        /**
         * $upstream_response_time
         *   keeps time spent on receiving the response from the upstream server;
         *   the time is kept in seconds with millisecond resolution. Times of
         *   several responses are separated by commas and colons like addresses
         *   in the $upstream_addr variable.
         */

        if (groupValue.contains(",")) {
            return groupValue.split(",");
        } else if (groupValue.contains(":")) {
            return groupValue.split(":");
        } else {
            String[] parts = {groupValue};
            return parts;
        }
    }

    private boolean isNotValueEmpty(String value) {
        return !("-".equals(value.trim()) || "".equals(value.trim()));
    }

    private boolean isSeparatedValuesEmpty(String[] values) {
        /**
         * It is possible for $upstream_response_time to return with comma/colon separated values,
         * where one or more of its values are "-"
         */
        int nullCount = 0;
        for (String v : values) {
            if (!isNotValueEmpty(v)) {
                nullCount++;
            }
        }
        return values.length == nullCount;
    }
}
