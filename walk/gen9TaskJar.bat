cd out\production\walk\
jar cfm WebCrawlerTest.jar ..\..\..\Manifest9Task.txt ru\ifmo\rain\kurilenko\crawler
cd ..\..\..
move .\out\production\walk\WebCrawlerTest.jar .\
java -jar WebCrawlerTest.jar easy ru.ifmo.rain.kurilenko.crawler.WebCrawler
java -cp "out\production\walk" ru.ifmo.rain.kurilenko.crawler.WebCrawler http://www.kgeorgiy.info/courses/java-advanced/homeworks.html 3 2 4