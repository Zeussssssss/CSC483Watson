Our team decided to work on Watson, a Jeopardy champion. 

This project is set up to be a Maven project. Therefore, the easiest way to run it would be to set it up in Eclipse or IntelliJ, and then simply run the Watson.java file with no command-line. We always use it and would recommend it. 

If using the command line, then first compile the project by using ```mvn compile```, then run the Watson.java file using ```mvn exec:java -Dexec.mainClass=”Watson”```, which would result in a main menu getting displayed, signifying that the program has started. For the program to work, the index folder “wiki_index.lucene” should be in the same directory. 

If it is absent, then run IndexMaker.java  using```mvn exec:java -Dexec.mainClass=”IndexMaker”```. Please remember to change the location specified in the code for the folder containing the wiki pages before running the code. 

Questions.txt should be in the directory, so there is no need to change the path for it in the code. 

GitHub didn't allow us to add our api-key for OpenAI, so you would have to add your own for the improved implementation. 

