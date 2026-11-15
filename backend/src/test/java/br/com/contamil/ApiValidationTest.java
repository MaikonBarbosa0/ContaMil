package br.com.contamil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class ApiValidationTest {
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;

    @Test
    void rejectsInvalidMonthWithUsefulMessage() throws Exception {
        mvc.perform(get("/api/summary").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Informe o mês no formato AAAA-MM, por exemplo 2026-11."));
    }

    @Test
    void rejectsInvalidExpenseWithoutWritingToDatabase() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":" ","amount":-5.001,"date":"2026-11-11","categoryId":null}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void reportsMalformedDateAndMissingMonthAsBadRequests() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Compra","amount":50.00,"date":"data inválida","categoryId":2}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/summary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownExpense() throws Exception {
        mvc.perform(get("/api/expenses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Gasto não encontrado."));
    }

    @Test
    void createdRecurrenceCanBeReadFromItsLocationHeader() throws Exception {
        MvcResult created = mvc.perform(post("/api/recurrences").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Internet","amount":99.90,"startDate":"2099-01-31","categoryId":1}
                                """))
                .andExpect(status().isCreated()).andReturn();

        mvc.perform(get(created.getResponse().getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Internet"))
                .andExpect(jsonPath("$.category.id").value(1))
                .andExpect(jsonPath("$.category.name").value("Moradia"))
                .andExpect(jsonPath("$.category.color").isString())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.nextDate").value("2099-01-31"));
    }

    @Test
    void categoryCatalogHasOnlyTheTenApprovedNamesAndNoManagementEndpoints() throws Exception {
        var catalog = mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[9].id").value(10))
                .andExpect(jsonPath("$[10]").doesNotExist());
        List<String> names = List.of("Moradia", "Mercado", "Carro", "Restaurantes", "Lojas", "Saúde",
                "Educação", "Lazer", "Assinaturas", "Outros");
        for (int index = 0; index < names.size(); index++) {
            catalog.andExpect(jsonPath("$[" + index + "].name").value(names.get(index)));
        }
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Categoria nova\",\"color\":\"#112233\"}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/categories/1/color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Outro nome\",\"color\":\"#112233\"}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/categories/1/color"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsRenamingThroughTheColorEndpointWithoutChangingTheCategory() throws Exception {
        String originalColor = categories.findById(1L).orElseThrow().getColor();
        mvc.perform(patch("/api/categories/1/color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"#112233\",\"name\":\"Outro nome\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[0].name").value("Moradia"))
                .andExpect(jsonPath("$[0].color").value(originalColor));
    }

    @Test
    void rejectsFreeTextAndUnknownCategoryIdsInExpensesAndRecurrences() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Compra","amount":50.00,"date":"2026-11-11","category":"Moradia"}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Compra","amount":50.00,"date":"2026-11-11","categoryId":999999}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Selecione uma categoria existente na lista."));
        mvc.perform(post("/api/recurrences").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Internet","amount":50.00,"startDate":"2099-11-11","category":"Moradia"}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/recurrences").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Internet","amount":50.00,"startDate":"2099-11-11","categoryId":999999}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Selecione uma categoria existente na lista."));
    }

    @Test
    void updatesOnlyTheColorAndAcceptsSixHexadecimalDigits() throws Exception {
        mvc.perform(patch("/api/categories/2/color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"#abcdef\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Mercado"))
                .andExpect(jsonPath("$.color").value("#abcdef"));
        mvc.perform(patch("/api/categories/999999/color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"#112233\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidCategoryColors() throws Exception {
        for (String invalid : List.of("#abc", "red", "#11223344", "#GGGGGG", "112233", "")) {
            mvc.perform(patch("/api/categories/3/color").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"color\":\"" + invalid + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.color").exists());
        }
    }

    @Test
    void healthChecksDatabaseConnection() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
