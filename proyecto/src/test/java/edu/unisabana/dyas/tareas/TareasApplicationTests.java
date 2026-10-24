package edu.unisabana.dyas.tareas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TareasApplicationTests {

    @Autowired
    private TareaRepository repository;

    @Autowired
    private TareaController controller;

    @Test
    void creaYListaTareas() {
        Tarea creada = controller.crear(new Tarea("Contenerizar el proyecto"));

        assertThat(creada.getId()).isNotNull();
        assertThat(controller.listar()).extracting(Tarea::getTitulo).contains("Contenerizar el proyecto");
    }

    @Test
    void actualizaYEliminaTareas() {
        Tarea creada = repository.save(new Tarea("Escribir el Dockerfile"));
        Tarea cambios = new Tarea("Escribir el Dockerfile multi-stage");
        cambios.setCompletada(true);

        Tarea actualizada = controller.actualizar(creada.getId(), cambios).getBody();
        assertThat(actualizada.isCompletada()).isTrue();

        controller.eliminar(creada.getId());
        assertThat(repository.existsById(creada.getId())).isFalse();
    }
}
