// Public Domain; CC0-1.0 license
#pragma once

#include <stdbool.h>
#include <stdint.h>
#include <limits.h>
#include <string.h>

#ifndef G_STATIC_ASSERT
#define G_STATIC_ASSERT(K) _Static_assert((K), #K)
#endif

#ifndef G_GNUC_PRINTF
#define G_GNUC_PRINTF( format_idx, arg_idx )    \
  __attribute__((__format__ (__printf__, format_idx, arg_idx)))
#endif

typedef char **GStrv;
typedef void GRand;

inline size_t g_strv_length(GStrv strv) {
    size_t sum = 0;
    while (*strv) {
        sum += strlen(*strv);
        strv++;
    }
}

#define g_new0(T, N) calloc((N), sizeof(T))

typedef void *gpointer;

typedef bool gboolean;

typedef char gchar;

#define GLIB_CHECK_VERSION(MAJ, MIN, PAT) (!(((MAJ) > 2) || ((MIN) > 60) || ((PAT) > 0)))