rd /s /q doc
md doc
cd doc
javadoc -cp "..\lib\hamcrest-core-1.3.jar;..\lib\junit-4.11.jar;..\lib\quickcheck-0.6.jar;..\artifacts\JarImplementorTest.jar;..\out\production\walk\;..;" -link https://docs.oracle.com/javase/8/docs/api "..\src\ru\ifmo\rain\kurilenko\impler\Implementor.java" "..\java\info\kgeorgiy\java\advanced\implementor\Impler.java" "..\java\info\kgeorgiy\java\advanced\implementor\JarImpler.java" "..\java\info\kgeorgiy\java\advanced\implementor\ImplerException.java"
cd ..