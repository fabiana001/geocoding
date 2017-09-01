## Generating case classes from files in /src/xsd


Put XSD files into */src/main/xsd* and check if the directory "/src/sbt-scalaxb" exist, then run:

> sbt clean compile

For more details on *scalaxb* see [here](http://martiell.github.io/scalaxb/maven/generate-mojo.html)

**Note**
Scalaxb has some issue with tag having dash symbol, i.e. it turns into "u45".
For more details on this issue see [here](https://stackoverflow.com/questions/3654006/scala-jaxb-or-similar).