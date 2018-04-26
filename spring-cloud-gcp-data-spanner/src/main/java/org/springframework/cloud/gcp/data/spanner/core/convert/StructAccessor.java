/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractStructReader;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.common.collect.ImmutableMap;

/**
 * A convenience wrapper class around Struct to make reading columns easier without
 * knowing their type.
 *
 * @author Balint Pato
 */
class StructAccessor {

	// @formatter:off
	static final Map<Class, BiFunction<Struct, String, List>> readIterableMapping =
					new ImmutableMap.Builder<Class, BiFunction<Struct, String, List>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBooleanList)
					.put(Long.class, AbstractStructReader::getLongList)
					.put(String.class, AbstractStructReader::getStringList)
					.put(Double.class, AbstractStructReader::getDoubleList)
					.put(Timestamp.class, AbstractStructReader::getTimestampList)
					.put(Date.class, AbstractStructReader::getDateList)
					.put(ByteArray.class, AbstractStructReader::getBytesList)
					.put(Struct.class, AbstractStructReader::getStructList)
					.build();

	// @formatter:off
	static final Map<Class, BiFunction<Struct, String, ?>> singleItemReadMethodMapping =
					new ImmutableMap.Builder<Class, BiFunction<Struct, String, ?>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBoolean)
					.put(Long.class, AbstractStructReader::getLong)
					.put(String.class, AbstractStructReader::getString)
					.put(Double.class, AbstractStructReader::getDouble)
					.put(Timestamp.class, AbstractStructReader::getTimestamp)
					.put(Date.class, AbstractStructReader::getDate)
					.put(ByteArray.class, AbstractStructReader::getBytes)
					.put(double[].class, AbstractStructReader::getDoubleArray)
					.put(long[].class, AbstractStructReader::getLongArray)
					.put(boolean[].class, AbstractStructReader::getBooleanArray).build();

	private final SpannerTypeMapper spannerTypeMapper;

	private Struct struct;

	private Set<String> columnNamesIndex;

	StructAccessor(Struct struct) {
		this.struct = struct;
		this.spannerTypeMapper = new SpannerTypeMapper();
		this.columnNamesIndex = indexColumnNames();
	}

	public Object getSingleValue(String colName) {
		Type colType = this.struct.getColumnType(colName);
		Type.Code code = colType.getCode();
		Class sourceType = code.equals(Type.Code.ARRAY)
				? this.spannerTypeMapper.getArrayJavaClassFor(colType.getArrayElementType().getCode())
				: this.spannerTypeMapper.getSimpleJavaClassFor(code);
		BiFunction readFunction = singleItemReadMethodMapping.get(sourceType);
		return readFunction.apply(this.struct, colName);
	}

	public List getListValue(String colName) {
		Type.Code innerTypeCode = this.struct.getColumnType(colName).getArrayElementType().getCode();
		Class clazz = this.spannerTypeMapper.getSimpleJavaClassFor(innerTypeCode);
		BiFunction<Struct, String, List> readMethod = readIterableMapping.get(clazz);
		return readMethod.apply(this.struct, colName);
	}

	public boolean hasColumn(String columnName) {
		return this.columnNamesIndex.contains(columnName);
	}

	public boolean isNull(String columnName) {
		return this.struct.isNull(columnName);
	}

	private Set<String> indexColumnNames() {
		Set<String> cols = new HashSet<>();
		for (Type.StructField f : this.struct.getType().getStructFields()) {
			cols.add(f.getName());
		}
		return cols;
	}
}
