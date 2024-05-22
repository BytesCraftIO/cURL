# 📬 cURL Annotation for Spring (Java)
The **cURL** annotation helps you generate a Postman collection directly from your Spring Rest API code. By applying this annotation to your controller methods, you can automatically create detailed entries for each endpoint in your API.

## 🚀 How to Use
### 1. Add the Dependency
**Maven**

Add the following dependency to your `pom.xml` file:

```xml
<pom>
    <dependency>
      <groupId>io.bytescraft</groupId>
      <artifactId>cURL</artifactId>
      <version>0.0.2</version>
    </dependency>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven.compiler.plugin.version}</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>io.bytescraft</groupId>
                            <artifactId>cURL</artifactId>
                            <version>0.0.2</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</pom>
```

**Gradle**

Add the following dependency to your `build.gradle` file:

```groovy
implementation 'io.bytescraft:cURL:0.0.2'
annotationProcessor 'io.bytescraft:cURL:0.0.2'
```

### 2. Annotate Your Controller Methods

Use the **cURL** annotation on your controller methods to define the details for the Postman collection.

Example:
    
```java
@RestController
@RequestMapping("/message")
public class MessageController {

    @cURL(name = "Get Message", folder = "/v1")
    @GetMapping(value = "/{id}" , produces = "application/json")
    public ResponseDTO getMessage(@PathVariable("id") Long id){
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World" + id);
        return responseDTO;
    }

    @cURL(name = "Post Message")
    @PostMapping(consumes = "application/json", headers = "Accept=application/json")
    public ResponseDTO postMessage(@Valid @RequestBody RequestDTO requestDTO) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World " + requestDTO.getName());
        return responseDTO;
    }

    @cURL
    @PutMapping(value = "/{id}" , produces = "application/json", consumes = "application/json")
    public ResponseDTO putMessage(@PathVariable("id") Long id, @RequestBody @Valid RequestDTO requestDTO) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World " + requestDTO.getName());
        return responseDTO;
    }

    @cURL
    @DeleteMapping(value = "/{id}" , produces = "application/json")
    public ResponseDTO deleteMessage(@PathVariable("id") Long id) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World");
        return responseDTO;
    }

    @cURL
    @GetMapping(value = "/filter" , produces = "application/json")
    public ResponseDTO filterMessages(@RequestParam(name = "query") String query, @RequestParam(name = "page", required = false) int page, @RequestParam(name = "size", required = false) int size) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World");
        return responseDTO;
    }
}
```

### 3. 🛠️ Create cURL.config File (Optional)
   Create a cURL.config file at the `src` folder level in your Java project. This configuration file supports the following properties:

- **collection.name**: The name of the generated collection.
- **collection.description**: A description for the generated collection.

**Example cURL.config file:**

```properties
collection.name=cURL
collection.description=This is a collection of cURL API endpoints.
```

## 📂 Project Structure
Your project structure should look like this:

```scss
my-java-project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
├── cURL.config
├── pom.xml (for Maven projects)
└── build.gradle (for Gradle projects)
```

### 4. 🛠️ Generate Postman Collection
Once your methods are annotated, build your project to generate a Postman collection. The specifics of this process will depend on the integration details of the cURL annotation, which typically involves running a Maven or Gradle task to process the annotations and output the Postman collection.

## 📜 License
This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributions
Contributions are welcome! Please submit a pull request or open an issue to discuss your changes.

## 📞 Contact
For any issues or questions, please open an issue in this repository.