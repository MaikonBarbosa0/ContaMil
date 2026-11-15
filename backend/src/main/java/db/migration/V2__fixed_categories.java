package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Catálogo e paleta inicial aprovados pelo usuário; esta migração não deve ser alterada após aplicada. */
public class V2__fixed_categories extends BaseJavaMigration {
    private static final List<FixedCategory> CATALOG = List.of(
            new FixedCategory(1, "Moradia", "#8B65ED"),
            new FixedCategory(2, "Mercado", "#19A5A1"),
            new FixedCategory(3, "Carro", "#F1AB38"),
            new FixedCategory(4, "Restaurantes", "#ED6E88"),
            new FixedCategory(5, "Lojas", "#4C8DE7"),
            new FixedCategory(6, "Saúde", "#3B9E6F"),
            new FixedCategory(7, "Educação", "#6366F1"),
            new FixedCategory(8, "Lazer", "#D946EF"),
            new FixedCategory(9, "Assinaturas", "#06B6D4"),
            new FixedCategory(10, "Outros", "#7C8795"));

    @Override
    public Integer getChecksum() {
        return Objects.hash(1, CATALOG);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        validateLegacyNames(connection);

        // A validação ocorre antes de qualquer DDL, inclusive em bancos que não revertem DDL.
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE categories (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(80) NOT NULL UNIQUE,
                        color VARCHAR(7) NOT NULL CHECK (LENGTH(color) = 7)
                    )
                    """);
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO categories (id, name, color) VALUES (?, ?, ?)")) {
            for (FixedCategory category : CATALOG) {
                insert.setLong(1, category.id());
                insert.setString(2, category.name());
                insert.setString(3, category.color());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        convertReference(connection, "expenses");
        convertReference(connection, "recurrences");
    }

    private void validateLegacyNames(Connection connection) throws SQLException {
        Set<String> approved = CATALOG.stream().map(FixedCategory::name).collect(Collectors.toSet());
        Set<String> unknown = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT category FROM expenses UNION SELECT category FROM recurrences")) {
            while (result.next()) {
                String name = result.getString(1);
                if (!approved.contains(name)) {
                    unknown.add(name);
                }
            }
        }
        if (!unknown.isEmpty()) {
            throw new SQLException("Migração V2 interrompida: categorias fora do catálogo aprovado: "
                    + String.join(", ", unknown)
                    + ". Nenhum rótulo será criado, renomeado ou apagado automaticamente; confirme o mapeamento antes de migrar.");
        }
    }

    private void convertReference(Connection connection, String table) throws SQLException {
        // table só recebe os dois nomes constantes acima, nunca entrada do usuário.
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN category_id BIGINT");
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE " + table + " SET category_id = ? WHERE category = ?")) {
            for (FixedCategory category : CATALOG) {
                update.setLong(1, category.id());
                update.setString(2, category.name());
                update.addBatch();
            }
            update.executeBatch();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ALTER COLUMN category_id SET NOT NULL");
            statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + table
                    + "_category_fk FOREIGN KEY (category_id) REFERENCES categories(id)");
            statement.execute("ALTER TABLE " + table + " DROP COLUMN category");
        }
    }

    private record FixedCategory(long id, String name, String color) {
    }
}
