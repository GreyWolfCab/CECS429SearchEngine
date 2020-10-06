### Search Engine
Search Engine using: Positional Inverted Index, K-gram Index, Porter2Stemmer Algorithm

Dependencies:
- SparkJava w/ Thymeleaf-Template: `com.sparkjava:spark-template-thymeleaf:2.7.1`
- Gson: `gson-2.8.6.jar`
- libstemmer (porter2stemmer): `tartarus snowball-stemmer`

###How to run search engine: (2 Ways)
#### CMD text:
You can run our search engine in the command line by running `indexer.java`
1. Open up your favorite text-editor or IDE (project developed on `intellij`)
2. download and load all dependencies (noted above)
3. run `indexer.java` for a fully functional cmd text application

####Web UI: localhost
Another way is to run from your local machine at `http://localhost:4567/`
1. Open up text-editor or IDE (project developed on `intellij`)
2. download and load all dependencies (noted above)
3. run `WebUI.java` 
4. open a chrome window and go to `http://localhost:4567/`


<b>You must bring your own corpus to index and search from (local file directory needed)</b>



