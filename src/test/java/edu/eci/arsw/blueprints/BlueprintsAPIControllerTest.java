package edu.eci.arsw.blueprints;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.arsw.blueprints.controllers.BlueprintsAPIController;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlueprintsAPIController.class)
class BlueprintsAPIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlueprintsServices services;

    @Autowired
    private ObjectMapper objectMapper;

    private Blueprint blueprintJohn;
    private Blueprint blueprintJane;
    private Point point1;
    private Point point2;

    @BeforeEach
    void setUp() {
        point1 = new Point(0, 0);
        point2 = new Point(10, 10);

        blueprintJohn = new Blueprint("john", "house", Arrays.asList(point1, point2));
        blueprintJane = new Blueprint("jane", "garden", Arrays.asList(point2));
    }

    @Test
    void deberiaObtenerTodosLosBlueprints() throws Exception {
        Set<Blueprint> blueprints = new HashSet<>(Arrays.asList(blueprintJohn, blueprintJane));
        when(services.getAllBlueprints()).thenReturn(blueprints);
        mockMvc.perform(get("/api/v1/blueprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void deberiaObtenerBlueprintsPorAutor() throws Exception {
        Set<Blueprint> blueprintsJohn = new HashSet<>(Arrays.asList(blueprintJohn));
        when(services.getBlueprintsByAuthor("john")).thenReturn(blueprintsJohn);
        mockMvc.perform(get("/api/v1/blueprints/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].author").value("john"))
                .andExpect(jsonPath("$.data[0].name").value("house"));
    }

    @Test
    void deberiaRetornar404CuandoAutorNoExiste() throws Exception {
        when(services.getBlueprintsByAuthor("unknown")).thenThrow(new BlueprintNotFoundException("Autor no encontrado"));
        mockMvc.perform(get("/api/v1/blueprints/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Autor no encontrado"));
    }

    @Test
    void deberiaObtenerBlueprintEspecifico() throws Exception {
        when(services.getBlueprint("john", "house")).thenReturn(blueprintJohn);
        mockMvc.perform(get("/api/v1/blueprints/john/house"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.author").value("john"))
                .andExpect(jsonPath("$.data.name").value("house"))
                .andExpect(jsonPath("$.data.points.length()").value(2));
    }

    @Test
    void deberiaRetornar404CuandoBlueprintNoExiste() throws Exception {
        when(services.getBlueprint("john", "unknown")).thenThrow(new BlueprintNotFoundException("Blueprint no encontrado"));
        mockMvc.perform(get("/api/v1/blueprints/john/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Blueprint no encontrado"));
    }

    @Test
    void deberiaCrearNuevoBlueprint() throws Exception {
        List<Point> points = Arrays.asList(new Point(1, 1), new Point(2, 2));
        Blueprint nuevoBlueprint = new Blueprint("pedro", "casa", points);
        BlueprintsAPIController.NewBlueprintRequest request =
                new BlueprintsAPIController.NewBlueprintRequest("pedro", "casa", points);

        doNothing().when(services).addNewBlueprint(any(Blueprint.class));
        mockMvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("created"))
                .andExpect(jsonPath("$.data.author").value("pedro"))
                .andExpect(jsonPath("$.data.name").value("casa"));
    }

    @Test
    void deberiaRetornar400CuandoBlueprintYaExiste() throws Exception {
        List<Point> points = Arrays.asList(new Point(1, 1), new Point(2, 2));
        BlueprintsAPIController.NewBlueprintRequest request =
                new BlueprintsAPIController.NewBlueprintRequest("john", "house", points);

        doThrow(new BlueprintPersistenceException("Blueprint ya existe"))
                .when(services).addNewBlueprint(any(Blueprint.class));
        mockMvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Blueprint ya existe"));
    }

    @Test
    void deberiaRetornar400CuandoDatosInvalidos() throws Exception {
        BlueprintsAPIController.NewBlueprintRequest request =
                new BlueprintsAPIController.NewBlueprintRequest("", "casa", Arrays.asList(new Point(1, 1)));
        mockMvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deberiaAgregarPuntoABlueprintExistente() throws Exception {
        Point nuevoPunto = new Point(5, 5);
        doNothing().when(services).addPoint("john", "house", 5, 5);
        mockMvc.perform(put("/api/v1/blueprints/john/house/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoPunto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.message").value("accepted"));
    }

    @Test
    void deberiaRetornar404AlAgregarPuntoABlueprintInexistente() throws Exception {
        Point nuevoPunto = new Point(5, 5);
        doThrow(new BlueprintNotFoundException("Blueprint no encontrado"))
                .when(services).addPoint("unknown", "unknown", 5, 5);
        mockMvc.perform(put("/api/v1/blueprints/unknown/unknown/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoPunto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Blueprint no encontrado"));
    }

    @Test
    void deberiaUsarCodigosHttpCorrectos() throws Exception {
        when(services.getAllBlueprints()).thenReturn(new HashSet<>());
        mockMvc.perform(get("/api/v1/blueprints")).andExpect(status().isOk());
        List<Point> points = Arrays.asList(new Point(1, 1));
        BlueprintsAPIController.NewBlueprintRequest request =
                new BlueprintsAPIController.NewBlueprintRequest("test", "test", points);
        mockMvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        Point point = new Point(1, 1);
        mockMvc.perform(put("/api/v1/blueprints/test/test/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(point)))
                .andExpect(status().isAccepted());
        when(services.getBlueprint(anyString(), anyString()))
                .thenThrow(new BlueprintNotFoundException("No encontrado"));
        mockMvc.perform(get("/api/v1/blueprints/x/y"))
                .andExpect(status().isNotFound());
    }
}