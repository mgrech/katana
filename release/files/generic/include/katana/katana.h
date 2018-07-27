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
#define KATANA_LIBRARY_EXPORT __declspec(dllexport)
#define KATANA_LIBRARY_IMPORT __declspec(dllimport)
#else
#define KATANA_LIBRARY_EXPORT
#define KATANA_LIBRARY_IMPORT
#endif

#ifdef KATANA_TYPE_LIBRARY
#define KATANA_EXPORT_TYPE KATANA_LIBRARY_EXPORT
#else
#define KATANA_EXPORT_TYPE
#endif

#define KEXPORT         KEXTERN __attribute__((visibility("default"))) KATANA_EXPORT_TYPE
#define KIMPORT         KEXTERN
#define KIMPORT_LIBRARY KEXTERN KATANA_LIBRARY_IMPORT

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
