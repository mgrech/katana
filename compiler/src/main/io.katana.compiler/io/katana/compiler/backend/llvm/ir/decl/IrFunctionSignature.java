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

package io.katana.compiler.backend.llvm.ir.decl;

import io.katana.compiler.backend.llvm.ir.type.IrType;

import java.util.ArrayList;
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

	public IrFunctionSignature(IrType returnType, String name,
	                           List<IrFunctionParameter> parameters)
	{
		this(Linkage.NONE, DllStorageClass.NONE, returnType, name, parameters);
	}

	private String attributesToString()
	{
		var attributes = new ArrayList<String>();

		if(linkage != Linkage.NONE)
			attributes.add(linkage.toString().toLowerCase());

		if(dllStorageClass != DllStorageClass.NONE)
			attributes.add(dllStorageClass.toString().toLowerCase());

		return attributes.isEmpty() ? "" : String.join(" ", attributes) + " ";
	}

	@Override
	public String toString()
	{
		var params = parameters.stream()
		                       .map(IrFunctionParameter::toString)
		                       .collect(Collectors.joining(", "));

		return String.format("%s%s @%s(%s)", attributesToString(), returnType, name, params);
	}
}
