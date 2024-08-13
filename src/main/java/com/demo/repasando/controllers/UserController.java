package com.demo.repasando.controllers;

import com.demo.repasando.entities.User;
import com.demo.repasando.services.UserService;
import com.demo.repasando.services.impl.UserServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Value("${pagination.page.size:10}") // Valor por defecto de tamaño de página
    private int defaultPageSize;

    private ResponseEntity<?> validation(BindingResult result) {
        // Mapear errores de validación
        Map<String, String> errores = result.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));

        // Retornar la respuesta con el código de estado 400
        return new ResponseEntity<>(errores, HttpStatus.BAD_REQUEST);
    }


    @GetMapping
    public ResponseEntity<List<User>> list(){
        List<User> users = userService.findAll();

        // Verificar si la lista está vacía
        if (users.isEmpty()) {
            // Retornar un estado 204 No Content si la lista está vacía
            return ResponseEntity.noContent().build();
        }

        // Retornar la lista de usuarios con estado 200 OK
        return ResponseEntity.ok(users);
    }


    @GetMapping("/page/{page}")
    public ResponseEntity<?> listPageable(@PathVariable Integer page, @RequestParam(value = "size", defaultValue = "${pagination.page.size:10}") Integer size) {

        // Validar el número de página
        if (page < 0) {
            Map<String, String> errorResponse = Collections.singletonMap(
                    "error",
                    "El número de página debe ser mayor o igual a 0."
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // Crear Pageable con número de página y tamaño de página
        Pageable pageable = PageRequest.of(page, size);

        // Obtener usuarios desde el servicio
        Page<User> users = userService.findAll(pageable);

        // Manejo de casos cuando la página está vacía
        if (users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(users);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getByIdUser(@PathVariable Long id) {
        Optional<User> userOptional = userService.findById(id);

        if (userOptional.isPresent()) {
            // Retornar el usuario con estado 200 OK
            return ResponseEntity.ok(userOptional.get());
        }

        // Retornar un estado 404 Not Found si el usuario no se encuentra
        Map<String, String> errorResponse = Collections.singletonMap(
                "error",
                "Usuario no encontrado por el id: " + id
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user, BindingResult result) {
        // Verificar si hay errores de validación
        if (result.hasErrors()) {
            return this.validation(result);
        }
        try {
            // Guardar el nuevo usuario y devolver la respuesta
            User newUser = userService.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (IllegalArgumentException e) {
            // Manejar excepción de argumento ilegal
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User user, BindingResult result) {
        // Verificación de errores de validación
        if (result.hasErrors()) {
            return this.validation(result);
        }

        // Buscar el usuario por ID
        Optional<User> userOptional = userService.findById(id);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "Usuario no encontrado por el id: " + id));
        }

        // Actualizar campos del usuario
        User userDb = userOptional.get();
        userDb.setUserName(user.getUserName());
        userDb.setName(user.getName());
        userDb.setLastName(user.getLastName());
        userDb.setEmail(user.getEmail());

        // Guardar el usuario actualizado
        User updatedUser = userService.save(userDb);
        return ResponseEntity.ok().body(updatedUser);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> userOptional = userService.findById(id);

        if (userOptional.isPresent()) {
            userService.deleteById(id);
            // Retornar un estado 204 No Content después de la eliminación exitosa
            return ResponseEntity.noContent().build();
        }

        // Retornar un estado 404 Not Found si el usuario no se encuentra
        Map<String, String> errorResponse = Collections.singletonMap(
                "error",
                "Usuario no encontrado por el id: " + id
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


}
