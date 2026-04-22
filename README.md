## PROJECT FOR GENERATING AUTOTESTS FROM AN OpenAPI FILE

### Installing ollama
curl -fsSL https://ollama.com/install.sh | sh

### Download the model
(using llama3.2:3b because we are limited to 16 GB of RAM)  
ollama pull llama3.2:3b

To prevent the model from running in the background constantly:  
Right-click on Ollama -> quit  
In settings, disable this application from background startup  
Find Ollama.app  
Right-click on it  
Select Show Package Contents  
Open the folder Contents → Library → LaunchAgents  
Find the file: com.ollama.ollama.plist  
Rename it by adding .disabled at the end  
The result will be: com.ollama.ollama.plist.disabled

### Start the model
(this occupies the terminal)  
ollama serve

### Stop the model after work
Ctrl + C

### Check that the model is working
(in another terminal)  
curl http://localhost:11434

### Running the application
In the IDE, configure the JDK to Java 21  
Run the application: click the RUN icon next to the main() method

### Running the AI agent
In the terminal, execute the command:  
curl -X POST http://localhost:8080/api/tests/generate  
Wait for the logs indicating that the test class generation has finished.  
The class will appear at: src/test/java/com/example/ai_test_generator

After checking and fixing the generated test class, run the tests as usual,  
passing your base server path for making requests to it in the application.properties file:  
api.base.url=