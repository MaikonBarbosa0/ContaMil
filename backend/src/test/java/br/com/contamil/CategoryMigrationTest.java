package br.com.contamil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

class CategoryMigrationTest {
    @Test
    void createsExactlyTheApprovedCatalogAndPaletteOnAnEmptyDatabase() throws Exception {
        String url = databaseUrl();
        migration(url, null).migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT id, name, color FROM categories ORDER BY id")) {
            List<String> actual = new ArrayList<>();
            while (result.next()) {
                actual.add(result.getLong(1) + ":" + result.getString(2) + ":" + result.getString(3));
            }
            assertThat(actual).containsExactly(
                    "1:Moradia:#8B65ED", "2:Mercado:#19A5A1", "3:Carro:#F1AB38", "4:Restaurantes:#ED6E88",
                    "5:Lojas:#4C8DE7", "6:Saúde:#3B9E6F", "7:Educação:#6366F1", "8:Lazer:#D946EF",
                    "9:Assinaturas:#06B6D4", "10:Outros:#7C8795");
        }
    }

    @Test
    void upgradesKnownLegacyLabelsWithoutLosingHistoryOrGenerationState() throws Exception {
        String url = databaseUrl();
        migration(url, "1").migrate();
        List<LegacyExpense> beforeExpenses;
        List<LegacyRecurrence> beforeRecurrences;
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO salary_versions (id,effective_month,amount) VALUES (17,'2026-01-01',3000.00)");
            statement.executeUpdate("""
                    INSERT INTO recurrences (id,description,amount,category,start_date,next_month,active) VALUES
                    (31,'Assinatura antiga',99.90,'Assinaturas','2026-01-31','2026-04-01',false),
                    (32,'Moradia mensal',20.00,'Moradia','2026-02-28','2026-03-01',true)
                    """);
            statement.executeUpdate("""
                    INSERT INTO expenses (id,description,amount,expense_date,category,recurrence_id,occurrence_month,deleted) VALUES
                    (81,'Compra',12.34,'2026-02-01','Mercado',null,null,false),
                    (82,'Ocorrência excluída',99.90,'2026-02-28','Assinaturas',31,'2026-02-01',true),
                    (83,'Ocorrência mantida',20.00,'2026-02-28','Moradia',32,'2026-02-01',false)
                    """);
            beforeExpenses = readExpenses(connection, false);
            beforeRecurrences = readRecurrences(connection, false);
        }

        migration(url, null).migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(readExpenses(connection, true)).containsExactlyElementsOf(beforeExpenses);
            assertThat(readRecurrences(connection, true)).containsExactlyElementsOf(beforeRecurrences);
            try (ResultSet result = statement.executeQuery("SELECT id, effective_month, amount FROM salary_versions")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("id")).isEqualTo(17);
                assertThat(result.getObject("effective_month", LocalDate.class)).isEqualTo(LocalDate.of(2026, 1, 1));
                assertThat(result.getBigDecimal("amount")).isEqualByComparingTo("3000.00");
                assertThat(result.next()).isFalse();
            }
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO expenses (description,amount,expense_date,category_id,deleted)
                    VALUES ('Inválido',1.00,'2026-03-01',999999,false)
                    """)).hasMessageContaining("999999");
        }
    }

    @Test
    void rejectsUnknownLegacyLabelsBeforeAnySchemaChangeIncludingHiddenHistory() throws Exception {
        String url = databaseUrl();
        migration(url, "1").migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO recurrences (description,amount,category,start_date,next_month,active)
                    VALUES ('Cancelada',10.00,'Categoria de recorrência desconhecida','2026-01-31','2026-02-01',false)
                    """);
            statement.executeUpdate("""
                    INSERT INTO expenses (description,amount,expense_date,category,deleted)
                    VALUES ('Excluído',5.00,'2026-01-31','Categoria de gasto desconhecida',true)
                    """);
        }

        assertThatThrownBy(() -> migration(url, null).migrate())
                .isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("categorias fora do catálogo aprovado")
                .hasStackTraceContaining("Categoria de gasto desconhecida")
                .hasStackTraceContaining("Categoria de recorrência desconhecida");

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(count(statement, "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'categories'"))
                    .isZero();
            assertThat(count(statement, "SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(column_name) = 'category_id'"))
                    .isZero();
            assertThat(readExpenses(connection, false)).singleElement().satisfies(expense -> {
                assertThat(expense.category()).isEqualTo("Categoria de gasto desconhecida");
                assertThat(expense.deleted()).isTrue();
            });
            assertThat(readRecurrences(connection, false)).singleElement().satisfies(recurrence -> {
                assertThat(recurrence.category()).isEqualTo("Categoria de recorrência desconhecida");
                assertThat(recurrence.active()).isFalse();
            });
        }
    }

    private Flyway migration(String url, String target) {
        var configuration = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private String databaseUrl() {
        return "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    }

    private int count(Statement statement, String query) throws Exception {
        try (ResultSet result = statement.executeQuery(query)) {
            result.next();
            return result.getInt(1);
        }
    }

    private List<LegacyExpense> readExpenses(Connection connection, boolean migrated) throws Exception {
        String query = "SELECT e.id,e.description,e.amount,e.expense_date,e.recurrence_id,e.occurrence_month,e.deleted,"
                + (migrated ? "c.name FROM expenses e JOIN categories c ON c.id=e.category_id" : "e.category FROM expenses e")
                + " ORDER BY e.id";
        List<LegacyExpense> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                rows.add(new LegacyExpense(result.getLong(1), result.getString(2), result.getBigDecimal(3),
                        result.getObject(4, LocalDate.class), result.getObject(5, Long.class),
                        result.getObject(6, LocalDate.class), result.getBoolean(7), result.getString(8)));
            }
        }
        return rows;
    }

    private List<LegacyRecurrence> readRecurrences(Connection connection, boolean migrated) throws Exception {
        String query = "SELECT r.id,r.description,r.amount,r.start_date,r.next_month,r.active,"
                + (migrated ? "c.name FROM recurrences r JOIN categories c ON c.id=r.category_id" : "r.category FROM recurrences r")
                + " ORDER BY r.id";
        List<LegacyRecurrence> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                rows.add(new LegacyRecurrence(result.getLong(1), result.getString(2), result.getBigDecimal(3),
                        result.getObject(4, LocalDate.class), result.getObject(5, LocalDate.class),
                        result.getBoolean(6), result.getString(7)));
            }
        }
        return rows;
    }

    private record LegacyExpense(long id, String description, BigDecimal amount, LocalDate date,
                                 Long recurrenceId, LocalDate occurrenceMonth, boolean deleted, String category) {
    }

    private record LegacyRecurrence(long id, String description, BigDecimal amount, LocalDate startDate,
                                    LocalDate nextMonth, boolean active, String category) {
    }
}
