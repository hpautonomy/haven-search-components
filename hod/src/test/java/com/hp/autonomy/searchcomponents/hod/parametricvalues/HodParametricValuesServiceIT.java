/*
 * (c) Copyright 2015-2017 Micro Focus or one of its affiliates.
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

package com.hp.autonomy.searchcomponents.hod.parametricvalues;

import com.hp.autonomy.hod.client.api.resource.ResourceName;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.searchcomponents.core.parametricvalues.AbstractParametricValuesServiceIT;
import com.hp.autonomy.searchcomponents.hod.beanconfiguration.HavenSearchHodConfiguration;
import com.hp.autonomy.searchcomponents.hod.fields.HodFieldsRequest;
import com.hp.autonomy.searchcomponents.hod.fields.HodFieldsRequestBuilder;
import com.hp.autonomy.searchcomponents.hod.search.HodQueryRestrictions;
import com.hp.autonomy.searchcomponents.hod.search.HodQueryRestrictionsBuilder;
import com.hp.autonomy.types.requests.idol.actions.tags.FieldPath;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collection;

@SpringBootTest(classes = HavenSearchHodConfiguration.class)
public class HodParametricValuesServiceIT extends AbstractParametricValuesServiceIT<HodParametricRequest, HodFieldsRequest, HodFieldsRequestBuilder, HodQueryRestrictions, ResourceName, HodErrorException> {
    @Autowired
    private ObjectFactory<HodQueryRestrictionsBuilder> queryRestrictionsBuilderFactory;

    @Override
    protected HodFieldsRequestBuilder fieldsRequestParams(final HodFieldsRequestBuilder fieldsRequestBuilder) {
        return fieldsRequestBuilder.databases(testUtils.buildQueryRestrictions().getDatabases());
    }

    @Override
    protected FieldPath determinePaginatableField() {
        // Can't use value details for non-numeric fields on HOD, but we know the tests use wiki_eng
        return tagNameFactory.getFieldPath("WIKIPEDIA_CATEGORY");
    }

    @Override
    protected HodParametricRequest noResultsParametricRequest(final Collection<FieldPath> fieldPaths) {
        final HodQueryRestrictions queryRestrictions = queryRestrictionsBuilderFactory.getObject()
            // No documents will match this text (probably)
            .queryText("sfoiewsfoseinf")
            .database(ResourceName.WIKI_ENG)
            .build();

        return parametricRequestBuilderFactory.getObject()
            .queryRestrictions(queryRestrictions)
            .fieldNames(fieldPaths)
            .build();
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    @Test(expected = NotImplementedException.class)
    public void getDependentParametricValues() throws HodErrorException {
        super.getDependentParametricValues();
    }
}
