/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import org.apache.commons.io.FileUtils;
import org.hjson.JsonValue;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
/**
 * Util class for Dynamic config helper module.
 */
public class DynamicConfigHelpers {

    public static final String TABLE_CONFIG_PATH = "tables" + File.separator;
    public static final String SECURITY_CONFIG_PATH = "security.hjson";
    public static final String VARIABLE_CONFIG_PATH = "variables.hjson";
    public static final String NEW_LINE = "\n";

    private static final String TABLE = "table";
    private static final String SECURITY = "security";
    private static final String VARIABLE = "variable";
    private static final String TABLE_SCHEMA = "/elideTableSchema.json";
    private static final String SECURITY_SCHEMA = "/elideSecuritySchema.json";
    private static final String VARIABLE_SCHEMA = "/elideVariableSchema.json";

    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.byDefault();
    private static JsonSchema tableSchema;
    private static JsonSchema securitySchema;
    private static JsonSchema variableSchema;

    static {
        tableSchema = loadSchema(TABLE_SCHEMA);
        securitySchema = loadSchema(SECURITY_SCHEMA);
        variableSchema = loadSchema(VARIABLE_SCHEMA);
    }

    /**
     * Checks whether input is null or empty.
     * @param input : input string
     * @return true or false
     */
    public static boolean isNullOrEmpty(String input) {
        return (input == null || input.trim().length() == 0);
    }

    /**
     * format config file path.
     * @param basePath : path to hjson config.
     * @return formatted file path.
     */
    public static String formatFilePath(String basePath) {
        String path = basePath;
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    /**
     * converts variable.hjson to map of variables.
     * @param basePath : root path to model dir
     * @return Map of variables
     * @throws JsonProcessingException
     * @throws ProcessingException
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getVariablesPojo(String basePath)
            throws JsonProcessingException, ProcessingException {
        String filePath = basePath + VARIABLE_CONFIG_PATH;
        File variableFile = new File(filePath);
        if (variableFile.exists()) {
            String jsonConfig = hjsonToJson(readConfigFile(variableFile));
            if (verifySchema(VARIABLE, jsonConfig)) {
                return getModelPojo(jsonConfig, Map.class);
            }
            return null;
        } else {
            log.info("Variables config file not found at " + filePath);
            return null;
        }
    }

    /**
     * converts all available table config to ElideTableConfig Pojo.
     * @param basePath : root path to model dir
     * @param variables : variables to resolve.
     * @return ElideTableConfig pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideTableConfig getElideTablePojo(String basePath, Map<String, Object> variables)
            throws IOException, ProcessingException {
        return getElideTablePojo(basePath, variables, TABLE_CONFIG_PATH);
    }

    /**
     * converts all available table config to ElideTableConfig Pojo.
     * @param basePath : root path to model dir
     * @param variables : variables to resolve.
     * @param tableDirName : dir name for table configs
     * @return ElideTableConfig pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideTableConfig getElideTablePojo(String basePath, Map<String, Object> variables,
            String tableDirName) throws IOException, ProcessingException {
        Collection<File> tableConfigs = FileUtils.listFiles(new File(basePath + tableDirName),
                new String[] {"hjson"}, false);
        Set<Table> tables = new HashSet<>();
        for (File tableConfig : tableConfigs) {
            ElideTableConfig table = stringToElideTablePojo(readConfigFile(tableConfig), variables);
            tables.addAll(table.getTables());
        }
        ElideTableConfig elideTableConfig = new ElideTableConfig();
        elideTableConfig.setTables(tables);
        return elideTableConfig;
    }

    /**
     * Generates ElideTableConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @return ElideTableConfig Pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideTableConfig stringToElideTablePojo(String content, Map<String, Object> variables)
            throws IOException, ProcessingException {
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        if (verifySchema(TABLE, jsonConfig)) {
            return getModelPojo(jsonConfig, ElideTableConfig.class);
        }
        return null;
    }

    /**
     * converts security.hjson to ElideSecurityConfig Pojo.
     * @param basePath : root path to model dir.
     * @param variables : variables to resolve.
     * @return ElideSecurityConfig Pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideSecurityConfig getElideSecurityPojo(String basePath, Map<String, Object> variables)
            throws IOException, ProcessingException {
        String filePath = basePath + SECURITY_CONFIG_PATH;
        File securityFile = new File(filePath);
        if (securityFile.exists()) {
            return stringToElideSecurityPojo(readConfigFile(securityFile), variables);
        } else {
            log.info("Security config file not found at " + filePath);
            return null;
        }
    }

    /**
     * Generates ElideSecurityConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @return ElideSecurityConfig Pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideSecurityConfig stringToElideSecurityPojo(String content, Map<String, Object> variables)
            throws IOException, ProcessingException {
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        if (verifySchema(SECURITY, jsonConfig)) {
            return getModelPojo(jsonConfig, ElideSecurityConfig.class);
        }
        return null;
    }

    /**
     * resolves variables in table and security config.
     * @param jsonConfig of table or security
     * @param variables map from config
     * @return json string with resolved variables
     * @throws IOException
     */
    public static String resolveVariables(String jsonConfig, Map<String, Object> variables) throws IOException {
        HandlebarsHydrator hydrator = new HandlebarsHydrator();
        return hydrator.hydrateConfigTemplate(jsonConfig, variables);
    }

    /**
     * Read hjson config file.
     * @param configFile : hjson file to read
     * @return hjson file content
     */
    public static String readConfigFile(File configFile) {
        StringBuffer sb = new StringBuffer();
        try {
            for (String line : FileUtils.readLines(configFile, StandardCharsets.UTF_8)) {
                sb.append(line);
                sb.append(NEW_LINE);
            }
        } catch (IOException e) {
            log.error("error while reading config file " + configFile.getName());
            log.error(e.getMessage());
        }
        return sb.toString();
    }

    private static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    private static <T> T getModelPojo(String jsonConfig, final Class<T> configPojo) throws JsonProcessingException {
        return new ObjectMapper().readValue(jsonConfig, configPojo);
    }

    private static boolean verifySchema(String configType, String jsonConfig)
            throws JsonMappingException, JsonProcessingException, ProcessingException {
        ProcessingReport results = null;

        switch (configType) {
        case TABLE :
            results = tableSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case SECURITY :
            results = securitySchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case VARIABLE :
            results = variableSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        }
        return results.isSuccess();
    }

    private static JsonSchema loadSchema(String resource) {
        ObjectMapper objectMapper = new ObjectMapper();
        Reader reader = new InputStreamReader(DynamicConfigHelpers.class.getResourceAsStream(resource));
        try {
            return FACTORY.getJsonSchema(objectMapper.readTree(reader));
        } catch (IOException e) {
            log.error("Error loading schema file " + resource + " to verify");
            log.error(e.getMessage());
        } catch (ProcessingException e) {
            log.error("Error loading schema file " + resource + " to verify");
            log.error(e.getMessage());
        }
        return null;
    }
}
