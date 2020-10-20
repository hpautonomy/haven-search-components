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

package com.hp.autonomy.searchcomponents.hod.search;

import com.hp.autonomy.hod.caching.CachingConfiguration;
import com.hp.autonomy.hod.client.api.resource.ResourceName;
import com.hp.autonomy.hod.client.api.textindex.query.search.Entity;
import com.hp.autonomy.hod.client.api.textindex.query.search.FindRelatedConceptsRequestBuilder;
import com.hp.autonomy.hod.client.api.textindex.query.search.FindRelatedConceptsService;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.hod.sso.HodAuthenticationPrincipal;
import com.hp.autonomy.searchcomponents.core.caching.CacheNames;
import com.hp.autonomy.searchcomponents.core.search.QueryRestrictions;
import com.hp.autonomy.searchcomponents.core.search.RelatedConceptsService;
import com.hpe.bigdata.frontend.spring.authentication.AuthenticationInformationRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hp.autonomy.searchcomponents.core.search.RelatedConceptsService.RELATED_CONCEPTS_SERVICE_BEAN_NAME;

/**
 * Default HoD implementation of {@link RelatedConceptsService}
 */
@Service(RELATED_CONCEPTS_SERVICE_BEAN_NAME)
class HodRelatedConceptsServiceImpl implements HodRelatedConceptsService {
    private final FindRelatedConceptsService findRelatedConceptsService;
    private final AuthenticationInformationRetriever<?, HodAuthenticationPrincipal> authenticationRetriever;

    @Autowired
    HodRelatedConceptsServiceImpl(
            final FindRelatedConceptsService findRelatedConceptsService,
            final AuthenticationInformationRetriever<?, HodAuthenticationPrincipal> authenticationRetriever) {
        this.findRelatedConceptsService = findRelatedConceptsService;
        this.authenticationRetriever = authenticationRetriever;
    }

    @Override
    @Cacheable(value = CacheNames.RELATED_CONCEPTS, cacheResolver = CachingConfiguration.PER_USER_CACHE_RESOLVER_NAME)
    public List<Entity> findRelatedConcepts(final HodRelatedConceptsRequest relatedConceptsRequest) throws HodErrorException {

        final QueryRestrictions<ResourceName> queryRestrictions = relatedConceptsRequest.getQueryRestrictions();
        final FindRelatedConceptsRequestBuilder params = new FindRelatedConceptsRequestBuilder()
                .setIndexes(queryRestrictions.getDatabases())
                .setFieldText(queryRestrictions.getFieldText())
                .setMinScore(queryRestrictions.getMinScore())
                .setMaxResults(relatedConceptsRequest.getMaxResults())
                .setSecurityInfo(authenticationRetriever.getPrincipal().getSecurityInfo());

        return findRelatedConceptsService.findRelatedConceptsWithText(queryRestrictions.getQueryText(), params);
    }
}
