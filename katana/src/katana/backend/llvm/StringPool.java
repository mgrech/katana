// Copyright 2017 Markus Grech
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

package katana.backend.llvm;

import katana.BuiltinType;
import katana.Limits;
import katana.backend.PlatformContext;
import katana.diag.CompileException;
import katana.platform.Arch;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class StringPool
{
	private final Map<String, String> namesByValue = new TreeMap<>();
	private int counter = 0;

	private String generateName()
	{
		return String.format("@.strpool.%s", counter++);
	}

	private byte[] utf8Encode(int cp)
	{
		StringBuilder builder = new StringBuilder();
		builder.appendCodePoint(cp);
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String escapeCharacter(int cp)
	{
		StringBuilder result = new StringBuilder();

		for(byte b : utf8Encode(cp))
			result.append(String.format("\\%02X", b));

		return result.toString();
	}

	private String escape(String s)
	{
		StringBuilder result = new StringBuilder();

		for(int cp : s.codePoints().toArray())
			if(cp == '"' || cp == '\\' || Character.isISOControl(cp))
				result.append(escapeCharacter(cp));
			else
				result.appendCodePoint(cp);

		return result.toString();
	}

	public String get(String value)
	{
		value = escape(value);
		String name = namesByValue.get(value);

		if(name != null)
			return name + ".ptr";

		name = generateName();
		namesByValue.put(value, name);
		return name + ".ptr";
	}

	private String encodeStringLength(BigInteger length, PlatformContext context)
	{
		BigInteger max = Limits.intMaxValue(BuiltinType.INT, context);

		if(length.compareTo(max) == 1)
			throw new CompileException("string too long for this platform");

		StringBuilder builder = new StringBuilder();

		Arch arch = context.target().arch;

		for(int i = 0; i != arch.pointerSize.intValue(); ++i)
		{
			builder.append('\\');
			builder.append(String.format("%02X", length.mod(BigInteger.valueOf(256)).intValue()));
			length = length.divide(BigInteger.valueOf(256));
		}

		return builder.toString();
	}

	public void generate(StringBuilder builder, PlatformContext context)
	{
		BigInteger lengthSize = context.target().arch.pointerSize;

		// length prefix + zero terminator
		BigInteger additionalLength = lengthSize.add(BigInteger.ONE);

		for(Map.Entry<String, String> entry : namesByValue.entrySet())
		{
			String name = entry.getValue();
			String value = entry.getKey();
			BigInteger valueLength = BigInteger.valueOf(value.length());
			BigInteger arrayLength = valueLength.add(additionalLength);
			String encodedLength = encodeStringLength(valueLength, context);

			String strfmt = "%s = private unnamed_addr constant [%s x i8] c\"%s%s\\00\", align %s\n";
			builder.append(String.format(strfmt, name, arrayLength, encodedLength, value, lengthSize));

			String getelementptrFmt = "getelementptr inbounds ([%s x i8], [%s x i8]* %s, i32 0, i32 %s)";
			String getelementptr = String.format(getelementptrFmt, arrayLength, arrayLength, name, lengthSize);
			String bitcast = String.format("bitcast (i8* %s to [%s x i8]*)", getelementptr, value.length());
			String ptrfmt = "%s.ptr = private unnamed_addr constant [%s x i8]* %s\n";
			builder.append(String.format(ptrfmt, name, value.length(), bitcast));
		}
	}
}
