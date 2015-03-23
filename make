echo "rm -rf build"
rm -rf build

echo "mkdir build"
mkdir build

echo "javac -d build java/com/hds/aw/commons/librsync/*.java"
javac -d build java/com/hds/aw/commons/librsync/*.java

echo "javah -classpath build -d build com.hds.aw.commons.librsync.LibrsyncWrapper"
javah -classpath build -d build com.hds.aw.commons.librsync.LibrsyncWrapper

echo "cp c/*.* build/."
cp c/*.* build/.

echo "cp lib/librsync.so.1 build/."
cp lib/librsync.so.1 build/.

echo "cd build"
cd build

echo "ln -s librsync.so.1 librsync.so"
ln -s librsync.so.1 librsync.so

echo "gcc -fPIC -c LibrsyncWrapper.c -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -I ."
gcc -fPIC -c LibrsyncWrapper.c -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -I .

echo "gcc LibrsyncWrapper.o -shared -o librsyncWrapper.so -Wl,-rpath,. -L. -lrsync"
gcc LibrsyncWrapper.o -shared -o librsyncWrapper.so -Wl,-rpath,. -L. -lrsync

echo "export LD_LIBRARY_PATH=."
export LD_LIBRARY_PATH=.

echo "java -Djava.library.path=. com.hds.aw.commons.librsync.LibrsyncWrapperTest ../data/file.base ../data/file.changed file.sig file.delta file.patch"
java -Djava.library.path=. com.hds.aw.commons.librsync.LibrsyncWrapperTest ../data/file.base ../data/file.changed file.sig file.delta file.patch

