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

package com.hp.autonomy.searchcomponents.hod.fields;

import com.hp.autonomy.searchcomponents.core.fields.AbstractFieldPathNormaliser;
import com.hp.autonomy.searchcomponents.core.fields.FieldPathNormaliser;
import com.hp.autonomy.searchcomponents.core.parametricvalues.ParametricValuesService;
import com.hp.autonomy.types.requests.idol.actions.tags.FieldPath;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.hp.autonomy.searchcomponents.core.fields.FieldPathNormaliser.FIELD_PATH_NORMALISER_BEAN_NAME;

/**
 * Default HoD implementation of {@link FieldPathNormaliser}.
 * There is no concept of field path normalisation in HoD.
 */
@Component(FIELD_PATH_NORMALISER_BEAN_NAME)
class HodFieldPathNormaliserImpl extends AbstractFieldPathNormaliser {
    @Override
    public FieldPath normaliseFieldPath(final String fieldPath) {
        if (StringUtils.isBlank(fieldPath)) {
            throw new IllegalArgumentException("Field names may not be blank or contain only forward slashes");
        }

        if (ParametricValuesService.AUTN_DATE_FIELD.equalsIgnoreCase(fieldPath)) {
            final String normalisedPath = fieldPath.toLowerCase();
            return newFieldPath(normalisedPath, normalisedPath);
        }

        return newFieldPath(fieldPath, fieldPath);
    }
}
