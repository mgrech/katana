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

#define KEXPORT __attribute__((visibility("default")))

typedef _Bool              kbool;
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
typedef kint64  kpint;
typedef kuint64 kupint;
#elif defined(KATANA_ARCH_X86)
typedef kint32  kint;
typedef kuint32 kuint;
typedef kint32  kpint;
typedef kuint32 kupint;
#endif

#define KSTATIC_ASSERT(expr) _Static_assert(expr, #expr);

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