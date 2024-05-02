#!/usr/bin/env bash
#
# AsteroidOSSync
# Copyright (c) 2024 AsteroidOS
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
set -Eeo pipefail
ANDROID_NDK_HOME=${ANDROID_NDK_HOME:?please supply a valid \$ANDROID_SDK_HOME}
ABI=${ABI:?please supply a valid android \$ABI}
case "${ABI}" in
  arm64-v8a)
    LINUX_ABI=aarch64
    ;;
  armeabi-v7a)
    LINUX_ABI=arm
    ;;
  x86_64)
    LINUX_ABI=x86_64
    ;;
  x86)
    LINUX_ABI=i686-pc
    ;;
  *)
    exit 1
    ;;
esac
SYSROOT=${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/sysroot

>&2 echo "Android NDK in ${ANDROID_NDK_HOME}"
export PATH=$PATH:${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin

PREFIX=${PREFIX:-/tmp/android-root/}

GLIB_VERSION=${GLIB_VERSION:-2.80.0}
GLIB_URL=https://download.gnome.org/sources/glib/${GLIB_VERSION%.*}/glib-${GLIB_VERSION}.tar.xz
GLIB_CACHE=${XDG_CACHE_DIR:-/tmp}/glib-${GLIB_VERSION}.tar.xz

LIBICONV_VERSION=${LIBICONV_VERSION:-1.17}
LIBICONV_URL=https://ftp.gnu.org/pub/gnu/libiconv/libiconv-${LIBICONV_VERSION}.tar.gz
LIBICONV_CACHE=${XDG_CACHE_DIR:-/tmp}/libiconv-${LIBICONV_VERSION}.tar.gz
export CFLAGS=--sysroot="${SYSROOT}"
export CPPFLAGS=--sysroot="${SYSROOT}"
export CC=${LINUX_ABI}-linux-android21-clang
export CXX=${LINUX_ABI}-linux-android21-clang++
export AR=llvm-ar
export RANLIB=llvm-ranlib

pushd "$(mktemp -d)"

  [[ ! -f "${LIBICONV_CACHE}" ]] \
    && wget -O "${LIBICONV_CACHE}" "${LIBICONV_URL}"
  bsdtar --strip-components=1 -xf "${LIBICONV_CACHE}"

  mkdir -p build
  pushd build

  ../configure --host=${LINUX_ABI}-linux-android --with-sysroot="${SYSROOT}" --prefix="${PREFIX}" --libdir="${PREFIX}/lib/${ABI}"
  make -j14
  make install

  popd # build

popd

pushd "$(mktemp -d)"

  [[ ! -f "${GLIB_CACHE}" ]] \
    && wget -O "${GLIB_CACHE}" "${GLIB_URL}"
  bsdtar --strip-components=1 -xf "${GLIB_CACHE}"

  >&2 echo "Will build GLib"

  _CROSS_FILE=$(mktemp)
  >&2 echo "Will setup cross"
  cat <<EOF. >"${_CROSS_FILE}"
[built-in options]
c_args = ['-I${PREFIX}/include']
c_link_args = ['-L${PREFIX}/lib/${ABI}']

[constants]
arch = '${LINUX_ABI}-linux-android'

[binaries]
ar = 'llvm-ar'
c = '${LINUX_ABI}-linux-android21-clang'
as = [c]
cpp = '${LINUX_ABI}-linux-android21-clang++'
ranlib = 'llvm-ranlib'
strip = 'llvm-strip'
pkgconfig = '/usr/bin/pkg-config'
cmake = '/usr/bin/cmake'

[properties]
sys_root = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/sysroot'
pkg_config_libdir = '${PREFIX}/lib/${ABI}/pkgconfig'

[host_machine]
system = 'android'
cpu_family = '${LINUX_ABI}'
cpu = '${LINUX_ABI}'
endian = 'little'
EOF.

  patch <<EOF. ||:
--- meson.build        2024-03-07 22:35:05.000000000 +0100
+++ meson.build        2024-04-27 11:44:38.569868768 +0200
@@ -2170 +2170 @@
-  libiconv = dependency('iconv')
+  libiconv = [cc.find_library('iconv', required : true, dirs : ['/work/android-root/lib'])]
EOF.


  >&2 echo "Will configure in ${PWD}/_builddir/"
  >&2 meson setup ./_builddir/ ./ --cross-file="${_CROSS_FILE}" --prefix="${PREFIX}" --libdir="lib/${ABI}"
  >&2 echo "Will build"
  >&2 ninja -C ./_builddir/
  >&2 echo "Will install"
  >&2 ninja -C ./_builddir/ install
  >&2 echo "All depends ready"

popd