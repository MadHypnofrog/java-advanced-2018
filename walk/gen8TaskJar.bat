cd out\production\walk\
jar cfm ParallelMapperTest.jar ..\..\..\Manifest8Task.txt ru\ifmo\rain\kurilenko\iterativeparallelism
cd ..\..\..
move .\out\production\walk\ParallelMapperTest.jar .\
java -jar ParallelMapperTest.jar list ru.ifmo.rain.kurilenko.iterativeparallelism.ParallelMapperImpl,ru.ifmo.rain.kurilenko.iterativeparallelism.IterativeParallelism