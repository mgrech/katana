// Copyright 2018 Markus Grech
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

package io.katana.compiler.backend.llvm.ir;

import java.util.List;
import java.util.stream.Collectors;

public class IrFunctionSignature
{
	public final Linkage linkage;
	public final DllStorageClass dllStorageClass;
	public final IrType returnType;
	public final String name;
	public final List<IrFunctionParameter> parameters;

	public IrFunctionSignature(Linkage linkage, DllStorageClass dllStorageClass, IrType returnType, String name,
	                           List<IrFunctionParameter> parameters)
	{
		this.linkage = linkage;
		this.dllStorageClass = dllStorageClass;
		this.returnType = returnType;
		this.name = name;
		this.parameters = parameters;
	}

	private String attributesToString()
	{
		var builder = new StringBuilder();
		builder.append(linkage.toString().toLowerCase());

		if(dllStorageClass != DllStorageClass.NONE)
		{
			builder.append(' ');
			builder.append(dllStorageClass.toString().toLowerCase());
		}

		return builder.toString();
	}

	@Override
	public String toString()
	{
		var params = parameters.stream()
		                       .map(IrFunctionParameter::toString)
		                       .collect(Collectors.joining(", "));

		return String.format("%s %s @%s(%s)", attributesToString(), returnType, name, params);
	}
}
