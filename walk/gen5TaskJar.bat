cd out\production\walk\
jar cfm JarImplementorTest.jar ..\..\..\Manifest5Task.txt ru\ifmo\rain\kurilenko\impler
cd ..\..\..
move .\out\production\walk\JarImplementorTest.jar .\
"C:\Program Files\Java\jdk1.8.0_101\bin\java.exe" -jar JarImplementorTest.jar jar-class ru.ifmo.rain.kurilenko.impler.Implementor