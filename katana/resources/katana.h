// Copyright 2016 Markus Grech
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

#if defined(_MSC_VER)
#define KATANA_COMPILER_MSVC
#elif defined(__clang__)
#define KATANA_COMPILER_CLANG
#elif defined(__GNUC__)
#define KATANA_COMPILER_GCC
#else
#error "compiler unsupported"
#endif

#ifdef __cplusplus
#define KEXTERNC extern "C"
#else
#define KEXTERNC
#endif

#if defined(KATANA_COMPILER_MSVC)
#define KEXPORT KEXTERNC
#elif defined(KATANA_COMPILER_CLANG) || defined(KATANA_COMPILER_GCC)
#define KEXPORT KEXTERNC __attribute__((visibility("default")))
#endif

#if defined(_M_X64) || defined(__x86_64__)
#define KATANA_ARCH_AMD64
#elif defined(_M_IX32) || defined(__i386__)
#define KATANA_ARCH_X86
#else
#error "architecture unsupported"
#endif

#if defined(__cplusplus)
typedef bool kbool;
#else
typedef _Bool kbool;
#endif

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
typedef void*              kptr;

#if defined(KATANA_ARCH_AMD64)
typedef kint64  kint;
typedef kuint64 kuint;
typedef kint64  kpint;
typedef kuint64 kupint;
#elif defined(KATANA_ARCH_X86)
typedef kint32  kint;
typedef kuint32 kuint;
typedef kint32  kpint;
typedef kuint32 kupint;
#endif

#ifdef __cplusplus
#define KSTATIC_ASSERT(expr) static_assert(expr, #expr);
#else
#define KSTATIC_ASSERT(expr) _Static_assert(expr, #expr);
#endif

KSTATIC_ASSERT(sizeof(kbool) == 1)

KSTATIC_ASSERT(sizeof(kint8)  == 1 && sizeof(kuint8)  == 1)
KSTATIC_ASSERT(sizeof(kint16) == 2 && sizeof(kuint16) == 2)
KSTATIC_ASSERT(sizeof(kint32) == 4 && sizeof(kuint32) == 4)
KSTATIC_ASSERT(sizeof(kint64) == 8 && sizeof(kuint64) == 8)

KSTATIC_ASSERT(sizeof(kpint) == sizeof(void*) && sizeof(kupint) == sizeof(void*))
KSTATIC_ASSERT(sizeof(void*) == sizeof(void(*)()))

KSTATIC_ASSERT(sizeof(kfloat32) == 4)
KSTATIC_ASSERT(sizeof(kfloat64) == 8)

#undef KSTATIC_ASSERT