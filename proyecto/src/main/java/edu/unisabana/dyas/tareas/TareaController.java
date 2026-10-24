package edu.unisabana.dyas.tareas;

import jakarta.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TareaController {

    private final TareaRepository repository;

    public TareaController(TareaRepository repository) {
        this.repository = repository;
    }

    // Muestra el contenedor que atendió la petición, igual que friendlyhello
    @GetMapping("/")
    public Map<String, Object> info() throws UnknownHostException {
        return Map.of(
                "aplicacion", "tareas",
                "hostname", InetAddress.getLocalHost().getHostName(),
                "tareas", repository.count());
    }

    @GetMapping("/tareas")
    public List<Tarea> listar() {
        return repository.findAll();
    }

    @GetMapping("/tareas/{id}")
    public ResponseEntity<Tarea> obtener(@PathVariable Long id) {
        return ResponseEntity.of(repository.findById(id));
    }

    @PostMapping("/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public Tarea crear(@Valid @RequestBody Tarea tarea) {
        return repository.save(new Tarea(tarea.getTitulo()));
    }

    @PutMapping("/tareas/{id}")
    public ResponseEntity<Tarea> actualizar(@PathVariable Long id, @Valid @RequestBody Tarea cambios) {
        return repository.findById(id).map(tarea -> {
            tarea.setTitulo(cambios.getTitulo());
            tarea.setCompletada(cambios.isCompletada());
            return ResponseEntity.ok(repository.save(tarea));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tareas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
