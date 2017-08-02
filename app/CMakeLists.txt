cmake_minimum_required(VERSION 3.4.1)
message(STATUS ABI_INFO  = ${ANDROID_ABI})

add_library( monerujo
             SHARED
             src/main/cpp/monerujo.cpp )

set(EXTERNAL_LIBS_DIR ${CMAKE_SOURCE_DIR}/../external-libs)

############
# OpenSSL
############

add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/openssl/lib/${ANDROID_ABI}/libcrypto.so)

add_library(ssl SHARED IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/openssl/lib/${ANDROID_ABI}/libssl.so)

############
# Boost
############

add_library(boost_chrono STATIC IMPORTED)
set_target_properties(boost_chrono PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_chrono.a)

add_library(boost_date_time STATIC IMPORTED)
set_target_properties(boost_date_time PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_date_time.a)

add_library(boost_filesystem STATIC IMPORTED)
set_target_properties(boost_filesystem PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_filesystem.a)

add_library(boost_program_options STATIC IMPORTED)
set_target_properties(boost_program_options PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_program_options.a)

add_library(boost_regex STATIC IMPORTED)
set_target_properties(boost_regex PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_regex.a)

add_library(boost_serialization STATIC IMPORTED)
set_target_properties(boost_serialization PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_serialization.a)

add_library(boost_system STATIC IMPORTED)
set_target_properties(boost_system PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_system.a)

add_library(boost_thread STATIC IMPORTED)
set_target_properties(boost_thread PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_thread.a)

add_library(boost_wserialization STATIC IMPORTED)
set_target_properties(boost_wserialization PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/boost/lib/${ANDROID_ABI}/libboost_wserialization.a)

#############
# Monero set(libs_to_merge wallet cryptonote_core cryptonote_basic mnemonics common cncrypto ringct)
#############

add_library(wallet SHARED IMPORTED)
set_target_properties(wallet PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libwallet.so)

add_library(cryptonote_core SHARED IMPORTED)
set_target_properties(cryptonote_core PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libcryptonote_core.so)

add_library(cryptonote_basic SHARED IMPORTED)
set_target_properties(cryptonote_basic PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libcryptonote_basic.so)

add_library(mnemonics SHARED IMPORTED)
set_target_properties(mnemonics PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libmnemonics.so)

add_library(common SHARED IMPORTED)
set_target_properties(common PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libcommon.so)

add_library(cncrypto SHARED IMPORTED)
set_target_properties(cncrypto PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libcncrypto.so)

add_library(ringct SHARED IMPORTED)
set_target_properties(ringct PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libringct.so)

#####

add_library(p2p SHARED IMPORTED)
set_target_properties(p2p PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libp2p.so)

add_library(blockchain_db SHARED IMPORTED)
set_target_properties(blockchain_db PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libblockchain_db.so)

add_library(lmdb SHARED IMPORTED)
set_target_properties(lmdb PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/liblmdb.so)

add_library(easylogging SHARED IMPORTED)
set_target_properties(easylogging PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libeasylogging.so)

add_library(unbound SHARED IMPORTED)
set_target_properties(unbound PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libunbound.so)

####
add_library(epee STATIC IMPORTED)
set_target_properties(epee PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libepee.a)

add_library(blocks STATIC IMPORTED)
set_target_properties(blocks PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libblocks.a)

add_library(miniupnpc STATIC IMPORTED)
set_target_properties(miniupnpc PROPERTIES IMPORTED_LOCATION
    ${EXTERNAL_LIBS_DIR}/monero/lib/${ANDROID_ABI}/libminiupnpc.a)

#############
# System
#############

find_library( log-lib log )

include_directories( ${EXTERNAL_LIBS_DIR}/monero/include )

message(STATUS EXTERNAL_LIBS_DIR :  ${EXTERNAL_LIBS_DIR})

target_link_libraries(  monerujo
                        wallet
                        cryptonote_core
                        cryptonote_basic
                        mnemonics
                        cncrypto
                        ringct
                        common
                        blockchain_db
                        lmdb
                        #easylogging # not for 0.10.3.1
                        unbound
                        p2p

                        epee
                        blocks
                        miniupnpc

                        boost_chrono
                        boost_date_time
                        boost_filesystem
                        boost_program_options
                        boost_regex
                        boost_serialization
                        boost_system
                        boost_thread
                        boost_wserialization

                        ssl
                        crypto

                       ${log-lib}
                       )
