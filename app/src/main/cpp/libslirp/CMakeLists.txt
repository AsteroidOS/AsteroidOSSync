cmake_minimum_required(VERSION 3.22.1)

project(libslirp)

add_library(slirp STATIC
        src/arp_table.c
        src/bootp.c
        src/cksum.c
        src/dhcpv6.c
        src/dnssearch.c
        src/if.c
        src/ip6_icmp.c
        src/ip6_input.c
        src/ip6_output.c
        src/ip_icmp.c
        src/ip_input.c
        src/ip_output.c
        src/mbuf.c
        src/misc.c
        src/ncsi.c
        src/ndp_table.c
        src/sbuf.c
        src/slirp.c
        src/socket.c
        src/state.c
        src/stream.c
        src/tcp_input.c
        src/tcp_output.c
        src/tcp_subr.c
        src/tcp_timer.c
        src/tftp.c
        src/udp.c
        src/udp6.c
        src/util.c
        src/version.c
        src/vmstate.c
        src/libvdeslirp.c
        )

find_package(PkgConfig REQUIRED)

pkg_check_modules(GLIB REQUIRED IMPORTED_TARGET glib-2.0)

target_include_directories(slirp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/src)
target_link_libraries(slirp PUBLIC PkgConfig::GLIB)