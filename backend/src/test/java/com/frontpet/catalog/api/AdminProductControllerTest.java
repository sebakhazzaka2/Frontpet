package com.frontpet.catalog.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.identity.domain.AdminUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD admin de productos (tarea 3.4), sobre el stack HTTP real.
 *
 * <p>El "usuario autenticado" es un {@link AdminUser} armado en memoria, sin
 * persistir — {@code SecurityMockMvcRequestPostProcessors.user(UserDetails)}
 * lo inyecta directo como principal, sin pasar por login real ni por
 * {@code admin_users} (que hoy no tiene ninguna fila sembrada). {@code
 * @WithMockUser} no serviría acá: arma un {@code User} de Spring genérico, y
 * {@code @AuthenticationPrincipal AdminUser} en el controller quedaría null.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminProductControllerTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired ObjectMapper objectMapper;

    private final AdminUser admin = adminFor(TENANT);

    @Test
    @DisplayName("POST sin cookie JWT devuelve 401")
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT sin cookie JWT devuelve 401")
    void updateRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/admin/products/" + UUID.randomUUID())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST presign sin cookie JWT devuelve 401")
    void presignRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products/images/presign")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST presign con mime não permitido devuelve 400")
    void presignRejectsDisallowedMimeType() throws Exception {
        String body = """
                {"fileName": "foto.pdf", "contentType": "application/pdf", "contentLength": 1024}
                """;

        mockMvc.perform(post("/api/v1/admin/products/images/presign")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Tipo de imagem não permitido: application/pdf. Use JPEG, PNG ou WebP."));
    }

    @Test
    @DisplayName("POST presign com tamanho acima de 5MB devuelve 400")
    void presignRejectsOversizedFile() throws Exception {
        String body = """
                {"fileName": "foto.jpg", "contentType": "image/jpeg", "contentLength": 6291456}
                """;

        mockMvc.perform(post("/api/v1/admin/products/images/presign")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST presign com payload válido devuelve URL firmada e publicUrl")
    void presignReturnsSignedUrl() throws Exception {
        String body = """
                {"fileName": "foto.png", "contentType": "image/png", "contentLength": 500000}
                """;

        mockMvc.perform(post("/api/v1/admin/products/images/presign")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.publicUrl").isNotEmpty())
                .andExpect(jsonPath("$.objectKey", org.hamcrest.Matchers.endsWith(".png")))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("DELETE sin cookie JWT devuelve 401")
    void deactivateRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/products/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST crea un producto sin variantes y devuelve 201 + publicId/slug")
    void createsSimpleProduct() throws Exception {
        String body = """
                {
                  "nome": "Ração Golden Admin 15kg",
                  "descricao": "Descrição de teste",
                  "price": 189.90,
                  "stock": 20,
                  "categorySlugs": ["racoes"],
                  "speciesSlugs": ["caes"]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.slug").value("racao-golden-admin-15kg"))
                .andExpect(jsonPath("$.price").value(189.90))
                .andExpect(jsonPath("$.categories[0].slug").value("racoes"))
                .andExpect(jsonPath("$.species[0].slug").value("caes"));
    }

    @Test
    @DisplayName("POST crea un producto con variantes, sin precio a nivel producto")
    void createsProductWithVariants() throws Exception {
        String body = """
                {
                  "nome": "Ração com Variantes Admin",
                  "variants": [
                    {"nomeVariante": "3kg", "price": 59.90, "stock": 5},
                    {"nomeVariante": "15kg", "price": 189.90, "stock": 3}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").doesNotExist())
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.variants[0].nomeVariante").value("3kg"));
    }

    @Test
    @DisplayName("POST sin variantes y sin precio da 400 con mensaje claro")
    void rejectsMissingPriceWithoutVariants() throws Exception {
        String body = """
                {"nome": "Produto Sem Preço Nem Variante"}
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Produto sem variantes precisa de um preço."));
    }

    @Test
    @DisplayName("POST con variantes Y precio en el producto da 400")
    void rejectsPriceWithVariants() throws Exception {
        String body = """
                {
                  "nome": "Produto Contraditório",
                  "price": 10.00,
                  "variants": [{"nomeVariante": "Único", "price": 10.00}]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Produto com variantes não deve informar preço no nível do produto."));
    }

    @Test
    @DisplayName("POST com priceOriginal > price cria produto em promoção")
    void createsProductOnSale() throws Exception {
        String body = """
                {"nome": "Produto Em Promoção", "price": 18.50, "priceOriginal": 24.90}
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(18.50))
                .andExpect(jsonPath("$.priceOriginal").value(24.90));
    }

    @Test
    @DisplayName("POST com priceOriginal <= price da 400")
    void rejectsPriceOriginalNotGreaterThanPrice() throws Exception {
        String body = """
                {"nome": "Produto Promoção Inválida", "price": 20.00, "priceOriginal": 20.00}
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Preço original deve ser maior que o preço de venda."));
    }

    @Test
    @DisplayName("PUT com priceOriginal null saca o produto de promoção (docs/pending-decisions.md §3)")
    void updateClearsPromotion() throws Exception {
        String createBody = """
                {"nome": "Produto Sai Da Promoção", "price": 18.50, "priceOriginal": 24.90}
                """;
        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priceOriginal").value(24.90))
                .andReturn().getResponse().getContentAsString();
        UUID publicId = UUID.fromString(objectMapper.readTree(createResponse).get("publicId").asText());

        String updateBody = """
                {"nome": "Produto Sai Da Promoção", "price": 24.90}
                """;

        mockMvc.perform(put("/api/v1/admin/products/" + publicId)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(24.90))
                .andExpect(jsonPath("$.priceOriginal").doesNotExist());
    }

    @Test
    @DisplayName("POST con nome en blanco da 400 con fieldErrors")
    void rejectsBlankName() throws Exception {
        String body = """
                {"nome": "  ", "price": 10.00}
                """;

        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nome").exists());
    }

    @Test
    @DisplayName("ciclo completo: crear → editar → desactivar")
    void fullLifecycle() throws Exception {
        String createBody = """
                {
                  "nome": "Ciclo Completo Admin",
                  "price": 50.00,
                  "stock": 10,
                  "categorySlugs": ["racoes"]
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID publicId = UUID.fromString(
                objectMapper.readTree(createResponse).get("publicId").asText());

        // Editar: nombre, precio, marca nueva (alta inline), categoría distinta.
        String updateBody = """
                {
                  "nome": "Ciclo Completo Editado",
                  "price": 75.00,
                  "stock": 5,
                  "brandNome": "Marca Nova Admin Test",
                  "categorySlugs": ["higiene"]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/products/" + publicId)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ciclo Completo Editado"))
                .andExpect(jsonPath("$.price").value(75.00))
                .andExpect(jsonPath("$.brandNome").value("Marca Nova Admin Test"))
                .andExpect(jsonPath("$.categories[0].slug").value("higiene"));

        // Desactivar: 204, y el endpoint público deja de encontrarlo (404).
        mockMvc.perform(delete("/api/v1/admin/products/" + publicId)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/ciclo-completo-editado"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT en producto con variantes rechaza precio a nivel producto")
    void updateRejectsPriceOnVariantProduct() throws Exception {
        String createBody = """
                {
                  "nome": "Produto Variante Para Editar",
                  "variants": [{"nomeVariante": "Único", "price": 20.00}]
                }
                """;
        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID publicId = UUID.fromString(
                objectMapper.readTree(createResponse).get("publicId").asText());

        String updateBody = """
                {"nome": "Produto Variante Para Editar", "price": 99.00}
                """;

        mockMvc.perform(put("/api/v1/admin/products/" + publicId)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(updateBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT con slug que colisiona con otro producto da 400")
    void updateRejectsSlugCollision() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"nome\": \"Produto Slug A\", \"price\": 10.00}"))
                .andExpect(status().isCreated());

        String secondResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"nome\": \"Produto Slug B\", \"price\": 10.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID secondId = UUID.fromString(
                objectMapper.readTree(secondResponse).get("publicId").asText());

        String updateBody = """
                {"nome": "Produto Slug B", "price": 10.00, "slug": "produto-slug-a"}
                """;

        mockMvc.perform(put("/api/v1/admin/products/" + secondId)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(updateBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /variants em produto sem variantes devuelve 400")
    void replaceVariantsRejectsSimpleProduct() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"nome\": \"Produto Simples Para Variantes\", \"price\": 10.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID publicId = UUID.fromString(objectMapper.readTree(createResponse).get("publicId").asText());

        mockMvc.perform(put("/api/v1/admin/products/" + publicId + "/variants")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("[{\"nomeVariante\": \"Único\", \"price\": 10.00}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /variants: actualiza existente, agrega nueva y soft-deletea la que falta")
    void replaceVariantsUpsertAndSoftDelete() throws Exception {
        String createBody = """
                {
                  "nome": "Produto Com Variantes Para Editar",
                  "variants": [
                    {"nomeVariante": "3kg", "price": 59.90, "stock": 5},
                    {"nomeVariante": "15kg", "price": 189.90, "stock": 3}
                  ]
                }
                """;
        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID publicId = UUID.fromString(objectMapper.readTree(createResponse).get("publicId").asText());
        long keptVariantId = objectMapper.readTree(createResponse).get("variants").get(0).get("id").asLong();
        // A "15kg" (variants[1]) no la mandamos en el próximo PUT: debe desaparecer (soft-delete).

        String replaceBody = """
                [
                  {"id": %d, "nomeVariante": "3kg Reformulado", "price": 65.00, "stock": 8},
                  {"nomeVariante": "30kg", "price": 349.90, "stock": 2}
                ]
                """.formatted(keptVariantId);

        mockMvc.perform(put("/api/v1/admin/products/" + publicId + "/variants")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(replaceBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.variants[*].nomeVariante",
                        org.hamcrest.Matchers.containsInAnyOrder("3kg Reformulado", "30kg")))
                .andExpect(jsonPath("$.variants[*].nomeVariante",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("15kg"))));
    }

    @Test
    @DisplayName("PUT /variants con id de otro produto devuelve 400")
    void replaceVariantsRejectsForeignVariantId() throws Exception {
        String firstBody = """
                {"nome": "Produto Variantes A", "variants": [{"nomeVariante": "Único", "price": 10.00}]}
                """;
        String firstResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(firstBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long foreignVariantId = objectMapper.readTree(firstResponse).get("variants").get(0).get("id").asLong();

        String secondBody = """
                {"nome": "Produto Variantes B", "variants": [{"nomeVariante": "Único", "price": 20.00}]}
                """;
        String secondResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(secondBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID secondPublicId = UUID.fromString(objectMapper.readTree(secondResponse).get("publicId").asText());

        String replaceBody = """
                [{"id": %d, "nomeVariante": "Roubado", "price": 99.00}]
                """.formatted(foreignVariantId);

        mockMvc.perform(put("/api/v1/admin/products/" + secondPublicId + "/variants")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(replaceBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /variants que deja el producto sin variantes ativas devuelve 400")
    void replaceVariantsRejectsZeroActiveResult() throws Exception {
        String createBody = """
                {"nome": "Produto Ficaria Sem Variantes", "variants": [{"nomeVariante": "Único", "price": 10.00}]}
                """;
        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID publicId = UUID.fromString(objectMapper.readTree(createResponse).get("publicId").asText());

        mockMvc.perform(put("/api/v1/admin/products/" + publicId + "/variants")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json").content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Produto precisa de ao menos uma variante ativa."));
    }

    private static AdminUser adminFor(UUID tenantId) {
        AdminUser user = new AdminUser();
        user.setId(1L);
        user.setTenantId(tenantId);
        user.setEmail("admin-test@frontpet.com.br");
        user.setPasswordHash("{bcrypt}$2a$10$fakehashfortestingonly");
        user.setRole("ADMIN");
        return user;
    }
}
