package io.bytescraft.controller;

import io.bytescraft.cURL;
import io.bytescraft.controller.request.RequestDTO;
import io.bytescraft.controller.response.ResponseDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * @author javaquery
 * @since 2024-04-30
 */
@RestController
@RequestMapping("/messageController")
public class MessageController {

    @cURL(name = "Get Message", folder = "/v1")
    @GetMapping(value = "/{id}" , produces = "application/json")
    public ResponseDTO getMessage(@PathVariable("id") Long id){
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMessage("Hello World" +  id);
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
