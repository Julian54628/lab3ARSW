package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
@Transactional
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final JdbcTemplate jdbc;

    @Autowired
    public PostgresBlueprintPersistence(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Point> pointMapper = (ResultSet rs, int rowNum) ->
            new Point(rs.getInt("x"), rs.getInt("y"));

    private boolean existsBlueprint(String author, String name) {
        Integer cnt = jdbc.queryForObject("SELECT count(*) FROM blueprints WHERE author = ? AND name = ?", Integer.class, author, name);
        return cnt != null && cnt > 0;
    }

    private List<Point> getPoints(String author, String name) {
        return jdbc.query("SELECT x, y FROM points WHERE author = ? AND bpname = ? ORDER BY id", pointMapper, author, name);
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        String author = bp.getAuthor();
        String name = bp.getName();
        if (existsBlueprint(author, name)) {
            throw new BlueprintPersistenceException("Blueprint already exists: " + author + ":" + name);
        }
        jdbc.update("INSERT INTO blueprints(author, name) VALUES(?, ?)", author, name);
        List<Point> pts = bp.getPoints();
        if (pts != null && !pts.isEmpty()) {
            for (Point p : pts) {
                jdbc.update("INSERT INTO points(author, bpname, x, y) VALUES(?, ?, ?, ?)",
                        author, name, p.x(), p.y());
            }
        }
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        try {
            Integer cnt = jdbc.queryForObject(
                    "SELECT count(*) FROM blueprints WHERE author = ? AND name = ?",
                    Integer.class, author, name);
            if (cnt == null || cnt == 0) throw new BlueprintNotFoundException("Blueprint not found: %s/%s".formatted(author, name));
            List<Point> pts = getPoints(author, name);
            return new Blueprint(author, name, pts);
        } catch (EmptyResultDataAccessException e) {
            throw new BlueprintNotFoundException("Blueprint not found: %s/%s".formatted(author, name));
        }
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<String> names = jdbc.query("SELECT name FROM blueprints WHERE author = ?", (rs, rowNum) -> rs.getString("name"), author);
        if (names.isEmpty()) throw new BlueprintNotFoundException("No blueprints for author: " + author);
        return names.stream().map(n -> new Blueprint(author, n, getPoints(author, n))).collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT author, name FROM blueprints");
        Set<Blueprint> set = new HashSet<>();
        for (Map<String, Object> r : rows) {
            String author = (String) r.get("author");
            String name = (String) r.get("name");
            set.add(new Blueprint(author, name, getPoints(author, name)));
        }
        return set;
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        if (!existsBlueprint(author, name)) {
            throw new BlueprintNotFoundException("Blueprint not found: %s/%s".formatted(author, name));
        }
        jdbc.update("INSERT INTO points(author, bpname, x, y) VALUES(?, ?, ?, ?)", author, name, x, y);
    }
}
