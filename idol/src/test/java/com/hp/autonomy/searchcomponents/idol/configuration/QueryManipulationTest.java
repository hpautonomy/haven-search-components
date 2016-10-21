/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.searchcomponents.idol.configuration;

import com.hp.autonomy.frontend.configuration.ConfigException;
import com.hp.autonomy.frontend.configuration.ConfigurationComponentTest;
import com.hp.autonomy.frontend.configuration.server.ServerConfig;
import com.hp.autonomy.types.requests.qms.actions.typeahead.params.ModeParam;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import java.io.IOException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class QueryManipulationTest extends ConfigurationComponentTest<QueryManipulation> {
    @Test(expected = ConfigException.class)
    public void validateBadConfig() throws ConfigException {
        QueryManipulation.builder()
                .enabled(true)
                .build()
                .basicValidate(null);
    }

    @Test
    public void disabled() throws ConfigException {
        QueryManipulation.builder()
                .build()
                .basicValidate(null);
    }

    @Override
    protected Class<QueryManipulation> getType() {
        return QueryManipulation.class;
    }

    @Override
    protected QueryManipulation constructComponent() {
        final ServerConfig serverConfig = ServerConfig.builder()
                .host("find-idol")
                .port(16000)
                .build();
        return QueryManipulation.builder()
                .server(serverConfig)
                .expandQuery(true)
                .blacklist("ISO_Blacklist")
                .enabled(true)
                .build();
    }

    @Override
    protected String sampleJson() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("/com/hp/autonomy/searchcomponents/idol/configuration/queryManipulation.json"));
    }

    @Override
    protected void validateJson(final JsonContent<QueryManipulation> jsonContent) {
        jsonContent.assertThat().hasJsonPathStringValue("@.server.host", "find-idol");
        jsonContent.assertThat().hasJsonPathNumberValue("@.server.port", 16000);
        jsonContent.assertThat().hasJsonPathBooleanValue("@.expandQuery", true);
        jsonContent.assertThat().hasJsonPathStringValue("@.blacklist", "ISO_Blacklist");
        jsonContent.assertThat().hasJsonPathBooleanValue("@.enabled", true);
    }

    @Override
    protected void validateParsedComponent(final ObjectContent<QueryManipulation> objectContent) {
        assertThat(objectContent.getObject().getServer().getProductType(), hasSize(3));
        objectContent.assertThat().hasFieldOrPropertyWithValue("typeAheadMode", ModeParam.Index);
        objectContent.assertThat().hasFieldOrPropertyWithValue("expandQuery", true);
        objectContent.assertThat().hasFieldOrPropertyWithValue("blacklist", "ISO_BLACKLIST");
        objectContent.assertThat().hasFieldOrPropertyWithValue("enabled", false);
    }

    @Override
    protected void validateMergedComponent(final ObjectContent<QueryManipulation> objectContent) {
        assertThat(objectContent.getObject().getServer().getProductType(), hasSize(3));
        objectContent.assertThat().hasFieldOrPropertyWithValue("typeAheadMode", ModeParam.Index);
        objectContent.assertThat().hasFieldOrPropertyWithValue("expandQuery", true);
        objectContent.assertThat().hasFieldOrPropertyWithValue("blacklist", "ISO_Blacklist");
        objectContent.assertThat().hasFieldOrPropertyWithValue("enabled", true);
    }

    @Override
    protected void validateString(final String objectAsString) {
        assertTrue(objectAsString.contains("blacklist"));
    }
}
