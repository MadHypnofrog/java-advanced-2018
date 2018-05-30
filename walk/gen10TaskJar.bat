cd out\production\walk\
jar cfm HelloUDPTest.jar ..\..\..\Manifest10Task.txt ru\ifmo\rain\kurilenko\helloudp
cd ..\..\..
move .\out\production\walk\HelloUDPTest.jar .\
java -jar HelloUDPTest.jar server ru.ifmo.rain.kurilenko.helloudp.HelloUDPServer
java -jar HelloUDPTest.jar client ru.ifmo.rain.kurilenko.helloudp.HelloUDPClient