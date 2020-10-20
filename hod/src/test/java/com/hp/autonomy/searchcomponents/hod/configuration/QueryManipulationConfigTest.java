/*
 * (c) Copyright 2015 Micro Focus or one of its affiliates.
 *
 * Licensed under the MIT License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are as may be set forth in the express warranty
 * statements accompanying such products and services. Nothing herein should be
 * construed as constituting an additional warranty. Micro Focus shall not be
 * liable for technical or editorial errors or omissions contained herein. The
 * information contained herein is subject to change without notice.
 */

package com.hp.autonomy.searchcomponents.hod.configuration;

import com.hp.autonomy.frontend.configuration.ConfigurationComponentTest;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class QueryManipulationConfigTest extends ConfigurationComponentTest<QueryManipulationConfig> {
    @Override
    protected Class<QueryManipulationConfig> getType() {
        return QueryManipulationConfig.class;
    }

    @Override
    protected QueryManipulationConfig constructComponent() {
        return QueryManipulationConfig.builder()
                .profile("SomeProfile")
                .index("SomeIndex")
                .build();
    }

    @Override
    protected String sampleJson() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("/configuration/queryManipulation.json"));
    }

    @Override
    protected void validateJson(final JsonContent<QueryManipulationConfig> jsonContent) {
        jsonContent.assertThat().hasJsonPathStringValue("@.index", "SomeIndex");
        jsonContent.assertThat().hasJsonPathStringValue("@.profile", "SomeProfile");
    }

    @Override
    protected void validateParsedComponent(final ObjectContent<QueryManipulationConfig> objectContent) {
        objectContent.assertThat().hasFieldOrPropertyWithValue("index", "search_default_index");
        objectContent.assertThat().hasFieldOrPropertyWithValue("profile", "search_default_profile");
    }

    @Override
    protected void validateMergedComponent(final ObjectContent<QueryManipulationConfig> objectContent) {
        objectContent.assertThat().hasFieldOrPropertyWithValue("index", "SomeIndex");
        objectContent.assertThat().hasFieldOrPropertyWithValue("profile", "SomeProfile");
    }

    @Override
    protected void validateString(final String objectAsString) {
        assertTrue(objectAsString.contains("profile"));
    }
}
