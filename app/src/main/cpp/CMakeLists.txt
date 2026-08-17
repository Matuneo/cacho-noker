cmake_minimum_required(VERSION 3.22.1)
project(clono_y_mejoro_mi_voz)

set(LAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/libmp3lame)
set(LAME_SOURCES
    ${LAME_DIR}/bitstream.c
    ${LAME_DIR}/encoder.c
    ${LAME_DIR}/fft.c
    ${LAME_DIR}/gain_analysis.c
    ${LAME_DIR}/id3tag.c
    ${LAME_DIR}/lame.c
    ${LAME_DIR}/mpglib_interface.c
    ${LAME_DIR}/newmdct.c
    ${LAME_DIR}/presets.c
    ${LAME_DIR}/psymodel.c
    ${LAME_DIR}/quantize.c
    ${LAME_DIR}/quantize_pvt.c
    ${LAME_DIR}/reservoir.c
    ${LAME_DIR}/set_get.c
    ${LAME_DIR}/tables.c
    ${LAME_DIR}/takehiro.c
    ${LAME_DIR}/util.c
    ${LAME_DIR}/vbrquantize.c
    ${LAME_DIR}/VbrTag.c
    ${LAME_DIR}/version.c
)

add_library(mp3bridge SHARED mp3_bridge.cpp ${LAME_SOURCES})
target_include_directories(mp3bridge PRIVATE ${LAME_DIR})
target_compile_definitions(mp3bridge PRIVATE STDC_HEADERS)
target_compile_options(mp3bridge PRIVATE -fPIC)

find_library(log-lib log)
target_link_libraries(mp3bridge ${log-lib})
