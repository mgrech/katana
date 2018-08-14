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

#pragma once

#define KATANA_VERSION "${project.version}"

#ifdef __cplusplus
#define KEXTERN extern "C"
#else
#define KEXTERN extern
#endif

#if defined(KATANA_OS_WINDOWS)
#define KATANA_LIBRARY_SHARED_EXPORT __declspec(dllexport)
#define KATANA_LIBRARY_SHARED_IMPORT __declspec(dllimport)
#else
#define KATANA_LIBRARY_SHARED_EXPORT
#define KATANA_LIBRARY_SHARED_IMPORT
#endif

#ifdef KATANA_TYPE_LIBRARY_SHARED
#define KATANA_EXPORT_TYPE KATANA_LIBRARY_SHARED_EXPORT
#else
#define KATANA_EXPORT_TYPE
#endif

#define KEXPORT        KEXTERN __attribute__((visibility("default"))) KATANA_EXPORT_TYPE
#define KIMPORT        KEXTERN
#define KIMPORT_SHARED KEXTERN KATANA_LIBRARY_SHARED_IMPORT

#ifdef __cplusplus
typedef bool               kbool;
#else
typedef _Bool              kbool;
#endif
typedef char               kbyte;
typedef signed char        kint8;
typedef unsigned char      kuint8;
typedef short              kint16;
typedef unsigned short     kuint16;
typedef int                kint32;
typedef unsigned           kuint32;
typedef long long          kint64;
typedef unsigned long long kuint64;
typedef float              kfloat32;
typedef double             kfloat64;

#if defined(KATANA_ARCH_AMD64)
typedef kint64  kint;
typedef kuint64 kuint;
#elif defined(KATANA_ARCH_X86)
typedef kint32  kint;
typedef kuint32 kuint;
#else
#error "unsupported arch"
#endif

#define KSLICE(...) struct { __VA_ARGS__* pointer; kint length; }

typedef KSLICE(void)     kslice;
typedef KSLICE(kbool)    kslice_bool;
typedef KSLICE(kbyte)    kslice_byte;
typedef KSLICE(kint8)    kslice_int8;
typedef KSLICE(kuint8)   kslice_uint8;
typedef KSLICE(kint16)   kslice_int16;
typedef KSLICE(kuint16)  kslice_uint16;
typedef KSLICE(kint32)   kslice_int32;
typedef KSLICE(kuint32)  kslice_uint32;
typedef KSLICE(kint64)   kslice_int64;
typedef KSLICE(kuint64)  kslice_uint64;
typedef KSLICE(kint)     kslice_int;
typedef KSLICE(kuint)    kslice_uint;
typedef KSLICE(kfloat32) kslice_float32;
typedef KSLICE(kfloat64) kslice_float64;

#ifdef __cplusplus
#define KSTATIC_ASSERT(expr) static_assert(expr, #expr);
#else
#define KSTATIC_ASSERT(expr) _Static_assert(expr, #expr);
#endif

KSTATIC_ASSERT(sizeof(kbool) == 1)
KSTATIC_ASSERT(sizeof(kbyte) == 1)
KSTATIC_ASSERT(sizeof(kint8)  == 1 && sizeof(kuint8)  == 1)
KSTATIC_ASSERT(sizeof(kint16) == 2 && sizeof(kuint16) == 2)
KSTATIC_ASSERT(sizeof(kint32) == 4 && sizeof(kuint32) == 4)
KSTATIC_ASSERT(sizeof(kint64) == 8 && sizeof(kuint64) == 8)

KSTATIC_ASSERT(sizeof(kint) == sizeof(void*) && sizeof(kuint) == sizeof(void*))
KSTATIC_ASSERT(sizeof(void*) == sizeof(void(*)()))

KSTATIC_ASSERT(sizeof(kfloat32) == 4)
KSTATIC_ASSERT(sizeof(kfloat64) == 8)

#undef KSTATIC_ASSERT
