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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Changski Tie Zheng Zhang, Du Xiao
 */
public class BeanUtil {

    private static boolean isToJsonIgnoreNull = true;
    private static boolean isToJsonPretty = false;

    protected static final ObjectMapper JacksonMapper = new ObjectMapper();
    protected static final ObjectMapper JacksonMapperIgnoreNull = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .setSerializationInclusion(Include.NON_EMPTY);
    protected final static XmlMapper xmlMapper = new XmlMapper();

    public static void registerModules(Module... modules) {
        xmlMapper.registerModules(modules);
        JacksonMapper.registerModules(modules);
        JacksonMapperIgnoreNull.registerModules(modules);
    }

    public static void configure(SerializationFeature f, boolean state) {
        xmlMapper.configure(f, state);
        JacksonMapper.configure(f, state);
        JacksonMapperIgnoreNull.configure(f, state);
    }

    public static void configure(DeserializationFeature f, boolean state) {
        xmlMapper.configure(f, state);
        JacksonMapper.configure(f, state);
        JacksonMapperIgnoreNull.configure(f, state);
    }

    public static void configure(JsonGenerator.Feature f, boolean state) {
        xmlMapper.configure(f, state);
        JacksonMapper.configure(f, state);
        JacksonMapperIgnoreNull.configure(f, state);
    }

    public static void configure(JsonParser.Feature f, boolean state) {
        xmlMapper.configure(f, state);
        JacksonMapper.configure(f, state);
        JacksonMapperIgnoreNull.configure(f, state);
    }

    public static void configure(boolean fromJsonFailOnUnknownProperties, boolean toJsonPretty, boolean toJsonIgnoreNull) {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, fromJsonFailOnUnknownProperties);
        isToJsonPretty = toJsonPretty;
        isToJsonIgnoreNull = toJsonIgnoreNull;
    }

    static {
        registerModules(new JavaTimeModule());
        //configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    /**
     * Serialization, convert to JSON string, not pretty and ignore null/empty
     *
     * @param <T>
     * @param obj
     * @return
     * @throws JsonProcessingException
     */
    public static <T extends Object> String toJson(T obj) throws JsonProcessingException {
        return toJson(obj, isToJsonPretty, isToJsonIgnoreNull);
    }

    /**
     * Serialization, convert to JSON string
     *
     * @param <T>
     * @param obj
     * @param pretty
     * @param ignoreNull
     * @return
     * @throws JsonProcessingException
     */
    public static <T extends Object> String toJson(T obj, boolean pretty, boolean ignoreNull) throws JsonProcessingException {
        if (obj == null) {
            return "";
        }

        if (pretty) {
            if (ignoreNull) {
                return JacksonMapperIgnoreNull.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } else {
                return JacksonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            }
        } else {
            if (ignoreNull) {
                return JacksonMapperIgnoreNull.writeValueAsString(obj);
            } else {
                return JacksonMapper.writeValueAsString(obj);
            }
        }
    }

    /**
     * Deserialization , convert JSON string to object T
     *
     * @param <T>
     * @param c
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public static <T extends Object> T fromJson(Class<T> c, String json) throws JsonProcessingException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JacksonMapper.readValue(json, c);
    }

    /**
     * Deserialization , convert JSON string to object T
     *
     * @param <T>
     * @param javaType
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public static <T extends Object> T fromJson(JavaType javaType, String json) throws JsonProcessingException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JacksonMapper.readValue(json, javaType);
    }

    public static <T extends Object> T fromJson(TypeReference<T> javaType, String json) throws JsonProcessingException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JacksonMapper.readValue(json, javaType);
    }

    /**
     * Deserialization , convert JSON string to object T
     *
     * @param <R>
     * @param collectionClass
     * @param genericClass
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public static <R extends Object> R fromJson(Class<R> collectionClass, Class<?> genericClass, String json) throws JsonProcessingException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        if (genericClass == null) {
            return fromJson(collectionClass, json);
        }
        JavaType javaType = JacksonMapper.getTypeFactory().constructParametricType(collectionClass, genericClass);
        return JacksonMapper.readValue(json, javaType);
    }

    public static <T extends Object> T fromXML(Class<T> targetClass, String xml) throws JsonProcessingException {
        return (T) xmlMapper.readValue(xml, targetClass);
    }

    public static String toXML(Object obj) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(obj);
    }

    public static final ValidatorFactory ValidatorFactory = Validation.buildDefaultValidatorFactory();

    public static String getBeanValidationResult(Object bean) {
        if (bean == null) {
            return "missing data";
        }
        Set<ConstraintViolation<Object>> violations = ValidatorFactory.getValidator().validate(bean);
        if (violations.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(bean.getClass().getSimpleName()).append(" Validation Failed: ");
        Iterator<ConstraintViolation<Object>> violationsIter = violations.iterator();
        while (violationsIter.hasNext()) {
            ConstraintViolation<Object> constViolation = violationsIter.next();
            sb.append(constViolation.getPropertyPath()).append("=").append(constViolation.getInvalidValue())
                    .append(" - ").append(constViolation.getMessage()).append("; ");

        }
        return sb.toString();
    }

//    public static String getSchema(Class<?> beanClass) {
//        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(JacksonMapper);
//        JsonSchema schema = schemaGen.generateSchema(beanClass);
//        String schemaString = JacksonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
//        System.out.println(schemaString);
//    }
}
