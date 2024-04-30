package io.bytescraft.controller;

import io.bytescraft.cURL;
import io.bytescraft.controller.request.RequestDTO;
import io.bytescraft.controller.response.ResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * @author javaquery
 * @since 2024-04-30
 */
@RestController
public class MessageController {

    @cURL(name = "Get Message")
    @GetMapping(value = "/message/{id}" , produces = "application/json")
    public ResponseDTO getMessage(@PathVariable("id") Long id) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World");
        return responseDTO;
    }

    @cURL(name = "Post Message")
    @PostMapping(value = "/message" , produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
    public ResponseDTO postMessage(@RequestBody @Valid RequestDTO requestDTO) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World " + requestDTO.getName());
        return responseDTO;
    }

    @PutMapping(value = "/message/{id}" , produces = "application/json", consumes = "application/json")
    public ResponseDTO putMessage(@PathVariable("id") Long id, @RequestBody @Valid RequestDTO requestDTO) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World " + requestDTO.getName());
        return responseDTO;
    }

    @DeleteMapping(value = "/message/{id}" , produces = "application/json")
    public ResponseDTO deleteMessage(@PathVariable("id") Long id) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World");
        return responseDTO;
    }
}
