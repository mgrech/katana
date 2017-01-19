// Copyright 2016-2017 Markus Grech
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package katana.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtils
{
	private static final ObjectMapper MAPPER = createMapper();

	private static ObjectMapper createMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
		mapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
		mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
		mapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
		mapper.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

		mapper.addHandler(new DeserializationProblemHandler()
		{
			@Override
			public Object handleWeirdStringValue(DeserializationContext context, Class<?> targetType, String value, String error) throws IOException
			{
				if(!targetType.isEnum())
					return NOT_HANDLED;

				try
				{
					Method valueOf = targetType.getMethod("valueOf", String.class);
					return valueOf.invoke(null, value.toUpperCase());
				}

				catch(InvocationTargetException ex)
				{
					if(ex.getTargetException() instanceof IllegalArgumentException)
						return NOT_HANDLED;

					Rethrow.of(ex.getTargetException());
					throw new AssertionError("unreachable");
				}

				catch(ReflectiveOperationException ex)
				{
					throw new RuntimeException(ex);
				}
			}
		});

		return mapper;
	}

	public static <T> T loadObject(Path path, Class<T> clazz) throws IOException
	{
		byte[] data = Files.readAllBytes(path);
		T result = MAPPER.readValue(data, clazz);

		if(result == null)
			throw new NullPointerException();

		return result;
	}
}
