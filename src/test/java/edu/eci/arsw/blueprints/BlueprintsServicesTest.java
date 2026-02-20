package edu.eci.arsw.blueprints;

import edu.eci.arsw.blueprints.filters.BlueprintsFilter;
import edu.eci.arsw.blueprints.filters.IdentityFilter;
import edu.eci.arsw.blueprints.filters.RedundancyFilter;
import edu.eci.arsw.blueprints.filters.UndersamplingFilter;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.InMemoryBlueprintPersistence;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas simples para BlueprintsServices
 */
class BlueprintsServicesTest {

    private BlueprintsServices services;
    private BlueprintPersistence persistence;
    private BlueprintsFilter identityFilter;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryBlueprintPersistence();
        identityFilter = new IdentityFilter();
        services = new BlueprintsServices(persistence, identityFilter);
    }


    @Test
    void deberiaObtenerTodosLosPlanos() {
        Set<Blueprint> planos = services.getAllBlueprints();
        assertNotNull(planos);
        assertEquals(3, planos.size()); // john:house, john:garage, jane:garden
    }

    @Test
    void deberiaObtenerPlanosPorAutor() throws Exception {
        Set<Blueprint> planosJohn = services.getBlueprintsByAuthor("john");
        assertEquals(2, planosJohn.size()); // john tiene 2 planos
    }

    @Test
    void deberiaObtenerPlanoEspecifico() throws Exception {
        Blueprint plano = services.getBlueprint("john", "house");
        assertNotNull(plano);
        assertEquals("john", plano.getAuthor());
        assertEquals("house", plano.getName());
        assertEquals(4, plano.getPoints().size()); // house tiene 4 puntos
    }

    @Test
    void deberiaLanzarExcepcionCuandoPlanoNoExiste() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            services.getBlueprint("autorInexistente", "planoInexistente");
        });
    }

    @Test
    void deberiaAgregarNuevoPlano() throws Exception {
        List<Point> puntos = Arrays.asList(new Point(1, 1), new Point(2, 2));
        Blueprint nuevoPlano = new Blueprint("pedro", "casa", puntos);
        services.addNewBlueprint(nuevoPlano);
        Blueprint planoGuardado = services.getBlueprint("pedro", "casa");
        assertNotNull(planoGuardado);
        assertEquals(2, planoGuardado.getPoints().size());
    }

    @Test
    void deberiaAgregarPuntoAPlanoExistente() throws Exception {
        services.addPoint("john", "house", 99, 99);
        Blueprint planoModificado = services.getBlueprint("john", "house");
        assertEquals(5, planoModificado.getPoints().size()); // tenía 4, ahora 5
        Point ultimoPunto = planoModificado.getPoints().get(planoModificado.getPoints().size() - 1);
        assertEquals(99, ultimoPunto.x());
        assertEquals(99, ultimoPunto.y());
    }

    @Test
    void filtroRedundancyDeberiaEliminarPuntosDuplicadosConsecutivos() throws Exception {
        BlueprintsFilter redundancyFilter = new RedundancyFilter();
        BlueprintsServices servicesWithRedundancy = new BlueprintsServices(persistence, redundancyFilter);
        List<Point> puntosConDuplicados = Arrays.asList(
                new Point(1, 1),
                new Point(1, 1),
                new Point(2, 2),
                new Point(2, 2),
                new Point(2, 2)
        );
        Blueprint planoConDuplicados = new Blueprint("test", "duplicados", puntosConDuplicados);
        persistence.saveBlueprint(planoConDuplicados);
        Blueprint resultado = servicesWithRedundancy.getBlueprint("test", "duplicados");
        assertEquals(2, resultado.getPoints().size()); // (1,1) y (2,2)
    }

    @Test
    void filtroUndersamplingDeberiaConservarUnoDeCadaDosPuntos() throws Exception {
        BlueprintsFilter undersamplingFilter = new UndersamplingFilter();
        BlueprintsServices servicesWithUndersampling = new BlueprintsServices(persistence, undersamplingFilter);
        List<Point> puntos = Arrays.asList(
                new Point(0, 0),  // índice 0 - se conserva
                new Point(1, 1),  // índice 1 - se elimina
                new Point(2, 2),  // índice 2 - se conserva
                new Point(3, 3),  // índice 3 - se elimina
                new Point(4, 4)   // índice 4 - se conserva
        );
        Blueprint plano = new Blueprint("test", "undersampling", puntos);
        persistence.saveBlueprint(plano);
        Blueprint resultado = servicesWithUndersampling.getBlueprint("test", "undersampling");
        assertEquals(3, resultado.getPoints().size()); // índices 0,2,4
    }
}