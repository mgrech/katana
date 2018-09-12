// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler;

public enum BuiltinType
{
	VOID   (Kind.VOID),
	BYTE   (Kind.BYTE),
	BOOL   (Kind.BOOL),
	INT8   (Kind.INT),
	INT16  (Kind.INT),
	INT32  (Kind.INT),
	INT64  (Kind.INT),
	INT    (Kind.INT),
	UINT8  (Kind.UINT),
	UINT16 (Kind.UINT),
	UINT32 (Kind.UINT),
	UINT64 (Kind.UINT),
	UINT   (Kind.UINT),
	FLOAT32(Kind.FLOAT),
	FLOAT64(Kind.FLOAT),
	NULL   (Kind.NULL);

	public enum Kind
	{
		VOID,
		BYTE,
		BOOL,
		INT,
		UINT,
		FLOAT,
		NULL,
	}

	public final Kind kind;

	BuiltinType(Kind kind)
	{
		this.kind = kind;
	}

	public static final BuiltinType[] INTEGER_TYPES =
	{
		INT, INT8, INT16, INT32, INT64,
		UINT, UINT8, UINT16, UINT32, UINT64,
	};

	public static final BuiltinType[] FLOAT_TYPES = {FLOAT32, FLOAT64};
}
