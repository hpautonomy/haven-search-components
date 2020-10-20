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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.hod.client.api.resource.ResourceName;
import com.hp.autonomy.hod.client.api.textindex.query.parametric.FieldRanges;
import com.hp.autonomy.hod.client.api.textindex.query.parametric.FieldValues;
import com.hp.autonomy.hod.client.api.textindex.query.parametric.GetParametricRangesRequestBuilder;
import com.hp.autonomy.hod.client.api.textindex.query.parametric.GetParametricRangesService;
import com.hp.autonomy.hod.client.api.textindex.query.parametric.GetParametricValuesRequestBuilder;
import com.hp.autonomy.hod.client.api.textindex.query.parametric.GetParametricValuesService;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.hod.sso.HodAuthenticationPrincipal;
import com.hp.autonomy.searchcomponents.core.fields.TagNameFactory;
import com.hp.autonomy.searchcomponents.core.parametricvalues.BucketingParams;
import com.hp.autonomy.searchcomponents.core.parametricvalues.BucketingParamsHelper;
import com.hp.autonomy.searchcomponents.core.test.CoreTestContext;
import com.hp.autonomy.searchcomponents.hod.configuration.HodSearchCapable;
import com.hp.autonomy.searchcomponents.hod.configuration.QueryManipulationConfig;
import com.hp.autonomy.searchcomponents.hod.fields.HodFieldsRequestBuilder;
import com.hp.autonomy.searchcomponents.hod.fields.HodFieldsService;
import com.hp.autonomy.searchcomponents.hod.requests.HodRequestBuilderConfiguration;
import com.hp.autonomy.searchcomponents.hod.search.HodQueryRestrictions;
import com.hp.autonomy.types.requests.idol.actions.tags.FieldPath;
import com.hp.autonomy.types.requests.idol.actions.tags.NumericRangeInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.NumericValueDetails;
import com.hp.autonomy.types.requests.idol.actions.tags.QueryTagCountInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.QueryTagInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.TagName;
import com.hp.autonomy.types.requests.idol.actions.tags.params.FieldTypeParam;
import com.hp.autonomy.types.requests.idol.actions.tags.params.SortParam;
import com.hpe.bigdata.frontend.spring.authentication.AuthenticationInformationRetriever;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hp.autonomy.searchcomponents.core.test.CoreTestContext.CORE_CLASSES_PROPERTY;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = {CoreTestContext.class, HodRequestBuilderConfiguration.class}, properties = CORE_CLASSES_PROPERTY)
public class HodParametricValuesServiceTest {
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Mock
    private GetParametricValuesService getParametricValuesService;

    @Mock
    private HodFieldsService fieldsService;

    @Autowired
    private ObjectFactory<HodFieldsRequestBuilder> fieldsRequestBuilderFactory;

    @Autowired
    private BucketingParamsHelper bucketingParamsHelper;

    @Autowired
    private TagNameFactory tagNameFactory;

    @Mock
    private ConfigService<HodSearchCapable> configService;

    @Mock
    private AuthenticationInformationRetriever<?, HodAuthenticationPrincipal> authenticationInformationRetriever;

    @Mock
    private HodAuthenticationPrincipal hodAuthenticationPrincipal;

    @Mock
    private HodSearchCapable config;

    @Mock
    private GetParametricRangesService getParametricRangesService;

    private HodParametricValuesService parametricValuesService;

    @SuppressWarnings("CastToConcreteClass")
    @Before
    public void setUp() throws HodErrorException {
        parametricValuesService = new HodParametricValuesServiceImpl(
            fieldsService,
            fieldsRequestBuilderFactory,
            getParametricValuesService(),
            getParametricRangesService,
            bucketingParamsHelper,
            tagNameFactory,
            configService,
            authenticationInformationRetriever
        );
    }

    @Before
    public void mocks() throws HodErrorException {
        when(config.getQueryManipulation()).thenReturn(QueryManipulationConfig.builder().profile("SomeProfile").index("SomeIndex").build());
        when(configService.getConfig()).thenReturn(config);

        when(hodAuthenticationPrincipal.getApplication()).thenReturn(new ResourceName("SomeDomain", "SomeIndex"));
        when(authenticationInformationRetriever.getPrincipal()).thenReturn(hodAuthenticationPrincipal);
    }

    @Test
    public void getsParametricValues() throws HodErrorException {
        final List<ResourceName> indexes = Arrays.asList(ResourceName.WIKI_ENG, ResourceName.PATENTS);

        final List<String> fieldNames = ImmutableList.<String>builder()
            .add("grassy field")
            .add("wasteland")
            .add("football field")
            .build();

        final HodParametricRequest request = generateRequest(indexes, fieldNames);
        final Set<QueryTagInfo> fieldNamesSet = parametricValuesService.getParametricValues(request);

        final Map<String, QueryTagInfo> fieldNamesMap = new HashMap<>();

        for(final QueryTagInfo parametricFieldName : fieldNamesSet) {
            fieldNamesMap.put(parametricFieldName.getDisplayName(), parametricFieldName);
        }

        assertThat(fieldNamesMap, hasKey("Grassy Field"));
        assertThat(fieldNamesMap, hasKey("Wasteland"));
        assertThat(fieldNamesMap, hasKey("Football Field"));

        assertThat(fieldNamesMap, not(hasKey("Empty Field")));

        final QueryTagInfo grassyField = fieldNamesMap.get("Grassy Field");

        assertThat(grassyField.getValues(), hasItem(new QueryTagCountInfo("snakes", "snakes", 33)));
    }

    @Test
    public void getsParametricValuesWithFilter() throws HodErrorException {
        final List<ResourceName> indexes = Arrays.asList(ResourceName.WIKI_ENG, ResourceName.PATENTS);

        final List<String> fieldNames = ImmutableList.<String>builder()
            .add("grassy field")
            .add("wasteland")
            .add("football field")
            .build();

        final HodParametricRequest request = generateRequest(indexes, fieldNames);
        when(request.getValueRestrictions()).thenReturn(Collections.singletonList("*LUG*"));
        when(request.getMaxValues()).thenReturn(5);
        final Set<QueryTagInfo> fieldNamesSet = parametricValuesService.getParametricValues(request);
        assertThat(fieldNamesSet, hasSize(1));
        final QueryTagInfo queryTagInfo = fieldNamesSet.iterator().next();
        assertThat(queryTagInfo.getId(), is("football field"));
        assertThat(queryTagInfo.getValues(), hasSize(1));
        assertThat(queryTagInfo.getValues().iterator().next().getValue(), is("slugs"));
    }

    @Test
    public void getsParametricValuesWithFilterAndMaxValues() throws HodErrorException {
        final List<ResourceName> indexes = Arrays.asList(ResourceName.WIKI_ENG, ResourceName.PATENTS);

        final List<String> fieldNames = ImmutableList.<String>builder()
            .add("grassy field")
            .add("wasteland")
            .add("football field")
            .build();

        final HodParametricRequest request = generateRequest(indexes, fieldNames);
        when(request.getValueRestrictions()).thenReturn(Collections.singletonList("*s*"));
        when(request.getMaxValues()).thenReturn(1);
        final Set<QueryTagInfo> fieldNamesSet = parametricValuesService.getParametricValues(request);
        assertThat(fieldNamesSet, hasSize(3));
        assertThat(fieldNamesSet.iterator().next().getValues(), hasSize(1));
    }

    @Test
    public void emptyFieldNamesReturnEmptyParametricValues() throws HodErrorException {
        final Map<FieldTypeParam, Set<TagName>> response = ImmutableMap.of(FieldTypeParam.Parametric, Collections.emptySet());
        when(fieldsService.getFields(any())).thenReturn(response);

        final List<ResourceName> indexes = Collections.singletonList(ResourceName.PATENTS);
        final HodParametricRequest testRequest = generateRequest(indexes, Collections.emptyList());
        final Set<QueryTagInfo> fieldNamesSet = parametricValuesService.getParametricValues(testRequest);
        assertThat(fieldNamesSet, is(empty()));
    }

    @Test
    public void lookupFieldNames() throws HodErrorException {
        final ImmutableMap<FieldTypeParam, Set<TagName>> fieldsResponse = ImmutableMap.of(FieldTypeParam.Parametric, Collections.singleton(tagNameFactory.buildTagName("grassy field")));
        when(fieldsService.getFields(any())).thenReturn(fieldsResponse);

        final List<ResourceName> indexes = Collections.singletonList(ResourceName.WIKI_ENG);
        final HodParametricRequest testRequest = generateRequest(indexes, Collections.emptyList());
        final Set<QueryTagInfo> fieldNamesSet = parametricValuesService.getParametricValues(testRequest);
        assertThat(fieldNamesSet, is(not(empty())));
    }

    @Test
    public void getValueDetailsNoFields() throws HodErrorException {
        final HodParametricRequest parametricRequest = generateRequest(Collections.singletonList(ResourceName.WIKI_ENG), Collections.emptyList());
        assertThat(parametricValuesService.getNumericValueDetails(parametricRequest).size(), is(0));
    }

    @Test
    public void getNumericValueDetails() throws HodErrorException {
        final String field = "MyField";
        final HodParametricRequest parametricRequest = generateRequest(Collections.singletonList(ResourceName.WIKI_ENG), Collections.singletonList(field));

        final List<FieldRanges> response = mockValueDetailsResponse(field, FieldRanges.ValueDetails.builder().minimum(0.8).maximum(21D).mean(6D).sum(54D).count(6).build());

        when(getParametricRangesService.getParametricRanges(
            anyCollectionOf(String.class),
            anyCollectionOf(ResourceName.class),
            any(String.class),
            any(GetParametricRangesRequestBuilder.class)
        )).thenReturn(response);

        final Map<FieldPath, NumericValueDetails> valueDetails = parametricValuesService.getNumericValueDetails(parametricRequest);
        assertThat(valueDetails.size(), is(1));
        assertThat(valueDetails, hasEntry(tagNameFactory.getFieldPath(field), NumericValueDetails.builder()
            .min(0.8)
            .max(21D)
            .average(6D)
            .sum(54D)
            .totalValues(6)
            .build()));
    }

    @Test
    public void getNumericParametricValuesInBuckets() throws HodErrorException {
        final List<FieldRanges> response = mockRangesResponse("ParametricNumericDateField", 4, Arrays.asList(
            FieldRanges.ValueRange.builder().count(2).lowerBound(3D).upperBound(4D).build(),
            FieldRanges.ValueRange.builder().count(0).lowerBound(4D).upperBound(5D).build(),
            FieldRanges.ValueRange.builder().count(0).lowerBound(5D).upperBound(6D).build(),
            FieldRanges.ValueRange.builder().count(1).lowerBound(6D).upperBound(7D).build(),
            FieldRanges.ValueRange.builder().count(0).lowerBound(7D).upperBound(8D).build(),
            FieldRanges.ValueRange.builder().count(0).lowerBound(8D).upperBound(9D).build(),
            FieldRanges.ValueRange.builder().count(0).lowerBound(9D).upperBound(10D).build(),
            FieldRanges.ValueRange.builder().count(0).lowerBound(10D).upperBound(11D).build(),
            FieldRanges.ValueRange.builder().count(1).lowerBound(11D).upperBound(12D).build()
        ));

        when(getParametricRangesService.getParametricRanges(
            anyCollectionOf(String.class),
            anyCollectionOf(ResourceName.class),
            any(String.class),
            any(GetParametricRangesRequestBuilder.class))
        ).thenReturn(response);

        final HodParametricRequest hodParametricRequest = generateRequest(Collections.singletonList(ResourceName.WIKI_ENG), Collections.singletonList("ParametricNumericDateField"));

        final List<NumericRangeInfo> results = parametricValuesService.getNumericParametricValuesInBuckets(
            hodParametricRequest,
            ImmutableMap.of(tagNameFactory.getFieldPath("ParametricNumericDateField"), new BucketingParams<>(9, 3.0, 12.0))
        );

        assertThat(results, hasSize(1));

        final NumericRangeInfo info = results.iterator().next();
        assertThat(info.getCount(), is(4));
        assertThat(info.getMin(), is(3d));
        assertThat(info.getMax(), is(12d));

        final Iterator<NumericRangeInfo.Value> iterator = info.getValues().iterator();
        assertEquals(new NumericRangeInfo.Value(3D, 4D, 2), iterator.next());
        assertEquals(new NumericRangeInfo.Value(4D, 5D, 0), iterator.next());
        assertEquals(new NumericRangeInfo.Value(5D, 6D, 0), iterator.next());
        assertEquals(new NumericRangeInfo.Value(6D, 7D, 1), iterator.next());
        assertEquals(new NumericRangeInfo.Value(7D, 8D, 0), iterator.next());
        assertEquals(new NumericRangeInfo.Value(8D, 9D, 0), iterator.next());
        assertEquals(new NumericRangeInfo.Value(9D, 10D, 0), iterator.next());
        assertEquals(new NumericRangeInfo.Value(10D, 11D, 0), iterator.next());
        assertEquals(new NumericRangeInfo.Value(11D, 12D, 1), iterator.next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNumericParametricValuesZeroBucketsZeroBuckets() throws HodErrorException {
        final HodParametricRequest hodParametricRequest = generateRequest(Collections.singletonList(ResourceName.WIKI_ENG), Collections.singletonList("ParametricNumericDateField"));
        parametricValuesService.getNumericParametricValuesInBuckets(hodParametricRequest, ImmutableMap.of(tagNameFactory.getFieldPath("ParametricNumericDateField"), new BucketingParams<>(0, 10.0, 11.0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNumericParametricValuesNoParams() throws HodErrorException {
        final HodParametricRequest hodParametricRequest = generateRequest(Collections.singletonList(ResourceName.WIKI_ENG), Collections.singletonList("ParametricNumericDateField"));
        parametricValuesService.getNumericParametricValuesInBuckets(hodParametricRequest, Collections.emptyMap());
    }

    @Test
    public void getNumericParametricValuesInBucketsNoFields() throws HodErrorException {
        final HodParametricRequest hodParametricRequest = generateRequest(Collections.singletonList(ResourceName.WIKI_ENG), Collections.singletonList("ParametricNumericDateField"));

        when(getParametricValuesService.getParametricValues(
            anyCollectionOf(String.class),
            anyCollectionOf(ResourceName.class),
            any(GetParametricValuesRequestBuilder.class)
        )).thenReturn(Collections.emptyList());

        final List<NumericRangeInfo> results = parametricValuesService.getNumericParametricValuesInBuckets(
            hodParametricRequest,
            ImmutableMap.of(tagNameFactory.getFieldPath("ParametricNumericDateField"), new BucketingParams<>(3, 1.5, 5.5))
        );

        MatcherAssert.assertThat(results, empty());
    }

    @Test(expected = NotImplementedException.class)
    public void dependentParametricValues() throws HodErrorException {
        parametricValuesService.getDependentParametricValues(mock(HodParametricRequest.class));
    }

    private HodParametricRequest generateRequest(final List<ResourceName> indexes, final Collection<String> fieldNames) {
        final HodQueryRestrictions queryRestrictions = mock(HodQueryRestrictions.class);
        when(queryRestrictions.getDatabases()).thenReturn(indexes);
        final HodParametricRequest parametricRequest = mock(HodParametricRequest.class);
        final List<FieldPath> fieldPaths = fieldNames.stream().map(tagNameFactory::getFieldPath).collect(Collectors.toList());
        when(parametricRequest.getFieldNames()).thenReturn(fieldPaths);
        when(parametricRequest.getQueryRestrictions()).thenReturn(queryRestrictions);
        when(parametricRequest.getSort()).thenReturn(SortParam.ReverseAlphabetical);
        when(parametricRequest.getStart()).thenReturn(1);
        return parametricRequest;
    }

    @SuppressWarnings("SameParameterValue")
    private List<FieldRanges> mockRangesResponse(final String field, final Integer count, final List<FieldRanges.ValueRange> valueRanges) {
        final FieldRanges.ValueDetails valueDetails = FieldRanges.ValueDetails.builder()
            .count(count)
            .build();

        final FieldRanges fieldRanges = FieldRanges.builder()
            .name(field)
            .valueDetails(valueDetails)
            .valueRanges(valueRanges)
            .build();

        return Collections.singletonList(fieldRanges);
    }

    private List<FieldRanges> mockValueDetailsResponse(final String field, final FieldRanges.ValueDetails valueDetails) {
        final FieldRanges.ValueRange range = FieldRanges.ValueRange.builder()
            .count(valueDetails.getCount())
            .lowerBound(valueDetails.getMinimum())
            .upperBound(valueDetails.getMaximum())
            .build();

        final FieldRanges fieldRanges = FieldRanges.builder()
            .name(field)
            .valueDetails(valueDetails)
            .valueRanges(Collections.singletonList(range))
            .build();

        return Collections.singletonList(fieldRanges);
    }

    private GetParametricValuesService getParametricValuesService() throws HodErrorException {
        final List<FieldValues.ValueAndCount> fieldsOfFootball = Arrays.asList(
            FieldValues.ValueAndCount.builder().value("worms").count(100).build(),
            FieldValues.ValueAndCount.builder().value("slugs").count(50).build()
        );

        final List<FieldValues.ValueAndCount> fieldsOfGrass = Arrays.asList(
            FieldValues.ValueAndCount.builder().value("birds").count(65).build(),
            FieldValues.ValueAndCount.builder().value("snakes").count(33).build()
        );

        final List<FieldValues.ValueAndCount> fieldsOfWaste = Arrays.asList(
            FieldValues.ValueAndCount.builder().value("humans").count(153).build(),
            FieldValues.ValueAndCount.builder().value("mutants").count(45).build()
        );

        final List<FieldValues> response = Arrays.asList(
            FieldValues.builder().name("football field").values(fieldsOfFootball).totalValues(200).build(),
            FieldValues.builder().name("grassy field").values(fieldsOfGrass).totalValues(98).build(),
            FieldValues.builder().name("wasteland").values(fieldsOfWaste).totalValues(198).build(),
            FieldValues.builder().name("empty field").build()
        );

        when(getParametricValuesService.getParametricValues(
            anyCollectionOf(String.class),
            anyCollectionOf(ResourceName.class),
            any(GetParametricValuesRequestBuilder.class)
        )).thenReturn(response);

        return getParametricValuesService;
    }
}
