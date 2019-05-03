// Copyright 2018-2019 Markus Grech
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

package io.katana.compiler.analysis;

import io.katana.compiler.Builtin;
import io.katana.compiler.BuiltinType;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

public class Builtins
{
	private static final EnumMap<Builtin, BuiltinDecl> DECLS = new EnumMap<>(Builtin.class);

	private static void registerPlainBuiltin(Builtin builtin, SemaType returnType, SemaType... paramTypes)
	{
		var signature = new OverloadedBuiltinDecl.Signature(List.of(paramTypes), returnType);
		var decl = new OverloadedBuiltinDecl(builtin, List.of(signature));
		DECLS.put(builtin, decl);
	}

	private static OverloadedBuiltinDecl.Signature createSignature(BuiltinType returnType, BuiltinType... paramTypes)
	{
		var semaParamTypes = Arrays.stream(paramTypes)
		                           .map(SemaTypeBuiltin::of)
		                           .map(SemaType.class::cast)
		                           .collect(Collectors.toList());
		var semaReturnType = SemaTypeBuiltin.of(returnType);
		return new OverloadedBuiltinDecl.Signature(semaParamTypes, semaReturnType);
	}

	static
	{
		for(var builtin : new Builtin[]{Builtin.ADD, Builtin.SUB, Builtin.MUL, Builtin.DIV, Builtin.REM})
		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : BuiltinType.INTEGER_TYPES)
				signatures.add(createSignature(integerType, integerType, integerType));

			for(var floatType : BuiltinType.FLOAT_TYPES)
				signatures.add(createSignature(floatType, floatType, floatType));

			DECLS.put(builtin, new OverloadedBuiltinDecl(builtin, signatures));
		}

		for(var builtin : new Builtin[]{Builtin.DIV_POW2})
		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : BuiltinType.INTEGER_TYPES)
				signatures.add(createSignature(integerType, integerType, integerType));

			DECLS.put(builtin, new OverloadedBuiltinDecl(builtin, signatures));
		}

		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : BuiltinType.INTEGER_TYPES)
				signatures.add(createSignature(integerType, integerType));

			for(var floatType : BuiltinType.FLOAT_TYPES)
				signatures.add(createSignature(floatType, floatType));

			DECLS.put(Builtin.NEG, new OverloadedBuiltinDecl(Builtin.NEG, signatures));
		}

		for(var builtin : new Builtin[]{Builtin.CMP_EQ, Builtin.CMP_NEQ, Builtin.CMP_LT, Builtin.CMP_LTE, Builtin.CMP_GT, Builtin.CMP_GTE})
		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : BuiltinType.INTEGER_TYPES)
				signatures.add(createSignature(BuiltinType.BOOL, integerType, integerType));

			for(var floatType : BuiltinType.FLOAT_TYPES)
				signatures.add(createSignature(BuiltinType.BOOL, floatType, floatType));

			DECLS.put(builtin, new OverloadedBuiltinDecl(builtin, signatures));
		}

		for(var builtin : new Builtin[]{Builtin.AND, Builtin.OR, Builtin.XOR, Builtin.SHL, Builtin.SHR, Builtin.ROL, Builtin.ROR})
		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : BuiltinType.INTEGER_TYPES)
				signatures.add(createSignature(integerType, integerType, integerType));

			DECLS.put(builtin, new OverloadedBuiltinDecl(builtin, signatures));
		}

		for(var builtin : new Builtin[]{Builtin.NOT, Builtin.CLZ, Builtin.CTZ, Builtin.POPCNT})
		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : BuiltinType.INTEGER_TYPES)
				signatures.add(createSignature(integerType, integerType));

			DECLS.put(builtin, new OverloadedBuiltinDecl(builtin, signatures));
		}

		{
			var signatures = new ArrayList<OverloadedBuiltinDecl.Signature>();

			for(var integerType : new BuiltinType[]{BuiltinType.INT16, BuiltinType.INT32, BuiltinType.INT64, BuiltinType.INT})
				signatures.add(createSignature(integerType, integerType));

			DECLS.put(Builtin.BSWAP, new OverloadedBuiltinDecl(Builtin.BSWAP, signatures));
		}

		var byteptr = Types.addNullablePointer(SemaTypeBuiltin.BYTE);
		var cbyteptr = Types.addNullablePointer(Types.addConst(SemaTypeBuiltin.BYTE));
		registerPlainBuiltin(Builtin.MEMCPY,  SemaTypeBuiltin.VOID, byteptr, cbyteptr, SemaTypeBuiltin.INT);
		registerPlainBuiltin(Builtin.MEMMOVE, SemaTypeBuiltin.VOID, byteptr, cbyteptr, SemaTypeBuiltin.INT);
		registerPlainBuiltin(Builtin.MEMSET,  SemaTypeBuiltin.VOID, byteptr, SemaTypeBuiltin.BYTE, SemaTypeBuiltin.INT);
	}

	public static Maybe<BuiltinDecl> tryFind(String name)
	{
		for(var builtin : Builtin.values())
			if(builtin.sourceName.equals(name))
				return Maybe.some(DECLS.get(builtin));

		return Maybe.none();
	}
}
