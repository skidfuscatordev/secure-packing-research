#!/bin/bash

set -e

LIB_NAME="driver-rs"
TARGETS=("x86_64-unknown-linux-gnu" "x86_64-pc-windows-gnu")
#TARGETS=("x86_64-pc-windows-gnu")
cd ./driver-rs

for TARGET in "${TARGETS[@]}"; do
    cross build --release --target $TARGET
done

cd ../

# Copy the shared libraries to the java_component/libs directory
mkdir -p ./driver-java/libs

cp ./driver-rs/target/x86_64-unknown-linux-gnu/release/libdriver_rs.so ./driver-java/libs/libdriver_rs_linux.so
cp ./driver-rs/target/x86_64-pc-windows-gnu/release/driver_rs.dll ./driver-java/libs/driver_rs_windows.dll