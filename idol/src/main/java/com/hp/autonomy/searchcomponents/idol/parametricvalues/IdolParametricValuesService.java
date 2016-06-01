/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.searchcomponents.idol.parametricvalues;

import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.Processor;
import com.autonomy.aci.client.util.AciParameters;
import com.hp.autonomy.aci.content.ranges.Range;
import com.hp.autonomy.aci.content.ranges.Ranges;
import com.hp.autonomy.idolutils.processors.AciResponseJaxbProcessorFactory;
import com.hp.autonomy.searchcomponents.core.caching.CacheNames;
import com.hp.autonomy.searchcomponents.core.fields.FieldsService;
import com.hp.autonomy.searchcomponents.core.parametricvalues.ParametricRequest;
import com.hp.autonomy.searchcomponents.core.parametricvalues.ParametricValuesService;
import com.hp.autonomy.searchcomponents.core.search.QueryRestrictions;
import com.hp.autonomy.searchcomponents.idol.fields.IdolFieldsRequest;
import com.hp.autonomy.searchcomponents.idol.search.HavenSearchAciParameterHandler;
import com.hp.autonomy.types.idol.FlatField;
import com.hp.autonomy.types.idol.GetQueryTagValuesResponseData;
import com.hp.autonomy.types.idol.RecursiveField;
import com.hp.autonomy.types.idol.TagValue;
import com.hp.autonomy.types.requests.idol.actions.tags.QueryTagCountInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.QueryTagInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.RangeInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.TagActions;
import com.hp.autonomy.types.requests.idol.actions.tags.params.FieldTypeParam;
import com.hp.autonomy.types.requests.idol.actions.tags.params.GetQueryTagValuesParams;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("WeakerAccess")
@Service
public class IdolParametricValuesService implements ParametricValuesService<IdolParametricRequest, String, AciErrorException> {
    static final String VALUE_NODE_NAME = "value";
    static final String VALUES_NODE_NAME = "values";
    static final String VALUE_MIN_NODE_NAME = "valuemin";
    static final String VALUE_MAX_NODE_NAME = "valuemax";

    private final HavenSearchAciParameterHandler parameterHandler;
    private final FieldsService<IdolFieldsRequest, AciErrorException> fieldsService;
    private final AciService contentAciService;
    private final Processor<GetQueryTagValuesResponseData> queryTagValuesResponseProcessor;

    @Autowired
    public IdolParametricValuesService(final HavenSearchAciParameterHandler parameterHandler, final FieldsService<IdolFieldsRequest, AciErrorException> fieldsService, final AciService contentAciService, final AciResponseJaxbProcessorFactory aciResponseProcessorFactory) {
        this.parameterHandler = parameterHandler;
        this.fieldsService = fieldsService;
        this.contentAciService = contentAciService;
        queryTagValuesResponseProcessor = aciResponseProcessorFactory.createAciResponseProcessor(GetQueryTagValuesResponseData.class);
    }

    @Override
    public Set<QueryTagInfo> getAllParametricValues(final IdolParametricRequest parametricRequest) throws AciErrorException {
        final Collection<String> fieldNames = new HashSet<>();
        fieldNames.addAll(parametricRequest.getFieldNames());
        if (fieldNames.isEmpty()) {
            fieldNames.addAll(fieldsService.getFields(new IdolFieldsRequest.Builder().build(), FieldTypeParam.Parametric).get(FieldTypeParam.Parametric));
        }

        final Set<QueryTagInfo> results;
        if (fieldNames.isEmpty()) {
            results = Collections.emptySet();
        } else {
            final List<FlatField> fields = getFlatFields(parametricRequest, fieldNames);
            results = new LinkedHashSet<>(fields.size());
            for (final FlatField field : fields) {
                final List<JAXBElement<? extends Serializable>> valueElements = field.getValueAndSubvalueOrValues();
                final LinkedHashSet<QueryTagCountInfo> values = new LinkedHashSet<>(valueElements.size());
                for (final JAXBElement<?> element : valueElements) {
                    if (VALUE_NODE_NAME.equals(element.getName().getLocalPart())) {
                        final TagValue tagValue = (TagValue) element.getValue();
                        values.add(new QueryTagCountInfo(tagValue.getValue(), tagValue.getCount()));
                    }
                }
                final String fieldName = getFieldNameFromPath(field.getName().get(0));
                if (!values.isEmpty()) {
                    results.add(new QueryTagInfo(fieldName, values));
                }
            }
        }

        return results;
    }

    @Override
    @Cacheable(CacheNames.PARAMETRIC_VALUES_IN_BUCKETS)
    public List<RangeInfo> getNumericParametricValuesInBuckets(final IdolParametricRequest parametricRequest, final int targetNumberOfBuckets) throws AciErrorException {
        final List<RangeInfo> results;
        if (parametricRequest.getFieldNames().isEmpty()) {
            results = Collections.emptyList();
        } else {
            final Map<String, RangeInfo> responseMap = getQueryMetadata(parametricRequest, targetNumberOfBuckets);

            final List<Range> ranges = generateRanges(responseMap, targetNumberOfBuckets);

            results = queryForRanges(parametricRequest, responseMap, ranges);
        }

        return results;
    }

    @Override
    public List<RecursiveField> getDependentParametricValues(final IdolParametricRequest parametricRequest) throws AciErrorException {
        final Collection<String> fieldNames = new ArrayList<>();
        fieldNames.addAll(parametricRequest.getFieldNames());
        if (fieldNames.isEmpty()) {
            fieldNames.addAll(fieldsService.getFields(new IdolFieldsRequest.Builder().build(), FieldTypeParam.Parametric).get(FieldTypeParam.Parametric));
        }

        final List<RecursiveField> results;
        if (fieldNames.isEmpty()) {
            results = Collections.emptyList();
        } else {
            final AciParameters aciParameters = createAciParameters(parametricRequest.getQueryRestrictions(), parametricRequest.isModified());

            aciParameters.add(GetQueryTagValuesParams.DocumentCount.name(), true);
            aciParameters.add(GetQueryTagValuesParams.FieldName.name(), StringUtils.join(fieldNames.toArray(), ','));
            aciParameters.add(GetQueryTagValuesParams.FieldDependence.name(), true);
            aciParameters.add(GetQueryTagValuesParams.FieldDependenceMultiLevel.name(), true);


            final GetQueryTagValuesResponseData responseData = contentAciService.executeAction(aciParameters, queryTagValuesResponseProcessor);

            results = responseData.getValues() == null ? Collections.<RecursiveField>emptyList() : responseData.getValues().getField();
        }

        return results;
    }

    private AciParameters createAciParameters(final QueryRestrictions<String> queryRestrictions, final boolean modified) {
        final AciParameters aciParameters = new AciParameters(TagActions.GetQueryTagValues.name());
        parameterHandler.addSearchRestrictions(aciParameters, queryRestrictions);
        if (modified) {
            parameterHandler.addQmsParameters(aciParameters, queryRestrictions);
        }
        return aciParameters;
    }

    private Map<String, RangeInfo> getQueryMetadata(final ParametricRequest<String> parametricRequest, final int targetNumberOfBuckets) {
        final AciParameters aciParameters = createAciParameters(parametricRequest.getQueryRestrictions(), parametricRequest.isModified());

        aciParameters.add(GetQueryTagValuesParams.MaxValues.name(), 1);
        aciParameters.add(GetQueryTagValuesParams.FieldName.name(), StringUtils.join(parametricRequest.getFieldNames().toArray(), ','));
        aciParameters.add(GetQueryTagValuesParams.ValueDetails.name(), true);

        final GetQueryTagValuesResponseData responseData = contentAciService.executeAction(aciParameters, queryTagValuesResponseProcessor);
        final Collection<FlatField> fields = responseData.getField();
        final Map<String, RangeInfo> responseMap = new HashMap<>(fields.size());
        for (final FlatField field : fields) {
            final List<JAXBElement<? extends Serializable>> valueElements = field.getValueAndSubvalueOrValues();
            final String fieldName = getFieldNameFromPath(field.getName().get(0));
            int count = 0;
            double minValue = 0.0;
            double maxValue = 0.0;
            for (final JAXBElement<?> element : valueElements) {
                final String elementLocalName = element.getName().getLocalPart();
                if (VALUES_NODE_NAME.equals(elementLocalName)) {
                    count = (Integer) element.getValue();
                } else if (VALUE_MIN_NODE_NAME.equals(elementLocalName)) {
                    minValue = (Float) element.getValue();
                } else if (VALUE_MAX_NODE_NAME.equals(elementLocalName)) {
                    maxValue = (Float) element.getValue();
                }
            }

            final double bucketSize = Math.ceil((maxValue - minValue + 1) / targetNumberOfBuckets);
            responseMap.put(fieldName, new RangeInfo(fieldName, count, minValue, maxValue, bucketSize, null));
        }
        return responseMap;
    }

    private List<Range> generateRanges(final Map<String, RangeInfo> responseMap, final int targetNumberOfBuckets) {
        final List<Range> ranges = new ArrayList<>(responseMap.size());
        for (final RangeInfo rangeInfo : responseMap.values()) {
            double value = Math.floor(rangeInfo.getMin());
            final List<Double> boundaries = new ArrayList<>(targetNumberOfBuckets);
            boundaries.add(value);
            while ((value += rangeInfo.getBucketSize()) <= rangeInfo.getMax()) {
                boundaries.add(value);
            }

            ranges.add(new Range(rangeInfo.getName(), ArrayUtils.toPrimitive(boundaries.toArray(new Double[boundaries.size()])), true));
        }
        return ranges;
    }

    private List<RangeInfo> queryForRanges(final ParametricRequest<String> parametricRequest, final Map<String, RangeInfo> responseMap, final List<Range> ranges) {
        final IdolParametricRequest bucketingRequest = new IdolParametricRequest.Builder()
                .setFieldNames(parametricRequest.getFieldNames())
                .setMaxValues(null)
                .setSort(parametricRequest.getSort())
                .setRanges(ranges)
                .setQueryRestrictions(parametricRequest.getQueryRestrictions())
                .setModified(parametricRequest.isModified())
                .build();
        final List<FlatField> flatFields = getFlatFields(bucketingRequest, parametricRequest.getFieldNames());
        final List<RangeInfo> results = new ArrayList<>(flatFields.size());
        for (final FlatField field : flatFields) {
            final String fieldName = getFieldNameFromPath(field.getName().get(0));
            final RangeInfo metadata = responseMap.get(fieldName);
            final double roundedMin = Math.floor(metadata.getMin());
            final double roundedMax = Math.floor(metadata.getMax()) + 1;

            final List<JAXBElement<? extends Serializable>> valueElements = field.getValueAndSubvalueOrValues();
            final Map<Double, RangeInfo.Value> values = new TreeMap<>();
            for (final JAXBElement<?> element : valueElements) {
                if (VALUE_NODE_NAME.equals(element.getName().getLocalPart())) {
                    final TagValue tagValue = (TagValue) element.getValue();
                    final String[] rangeValues = tagValue.getValue().split(",");
                    final double min = NumberUtils.toDouble(rangeValues[0], roundedMin);
                    final double max = rangeValues.length > 1 ? Double.parseDouble(rangeValues[1]) : roundedMax;
                    values.put(min, new RangeInfo.Value(tagValue.getCount(), min, max));
                }
            }

            for (double d = roundedMin; d < roundedMax; d += metadata.getBucketSize()) {
                if (!values.containsKey(d)) {
                    values.put(d, new RangeInfo.Value(0, d, d + metadata.getBucketSize()));
                }
            }

            if (!values.isEmpty()) {
                results.add(new RangeInfo(fieldName, metadata.getCount(), metadata.getMin(), metadata.getMax(), metadata.getBucketSize(), new ArrayList<>(values.values())));
            }
        }
        return results;
    }

    private List<FlatField> getFlatFields(final ParametricRequest<String> parametricRequest, final Collection<String> fieldNames) {
        final AciParameters aciParameters = new AciParameters(TagActions.GetQueryTagValues.name());
        parameterHandler.addSearchRestrictions(aciParameters, parametricRequest.getQueryRestrictions());

        if (parametricRequest.isModified()) {
            parameterHandler.addQmsParameters(aciParameters, parametricRequest.getQueryRestrictions());
        }

        aciParameters.add(GetQueryTagValuesParams.DocumentCount.name(), true);
        aciParameters.add(GetQueryTagValuesParams.MaxValues.name(), parametricRequest.getMaxValues());
        aciParameters.add(GetQueryTagValuesParams.FieldName.name(), StringUtils.join(fieldNames.toArray(), ','));
        aciParameters.add(GetQueryTagValuesParams.Sort.name(), parametricRequest.getSort());
        aciParameters.add(GetQueryTagValuesParams.Ranges.name(), new Ranges(parametricRequest.getRanges()));

        final GetQueryTagValuesResponseData responseData = contentAciService.executeAction(aciParameters, queryTagValuesResponseProcessor);
        return responseData.getField();
    }

    private String getFieldNameFromPath(final String value) {
        return value.contains("/") ? value.substring(value.lastIndexOf('/') + 1) : value;
    }
}
