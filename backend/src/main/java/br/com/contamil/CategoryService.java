package br.com.contamil;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CategoryService {
    private final CategoryRepository categories;

    CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<ApiModels.CategoryView> list() {
        return categories.findAllByOrderByIdAsc().stream().map(ApiModels.CategoryView::from).toList();
    }

    @Transactional
    public ApiModels.CategoryView updateColor(Long id, ApiModels.CategoryColorInput input) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
        category.setColor(input.color());
        return ApiModels.CategoryView.from(category);
    }

    Category forInput(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new InvalidInputException("Selecione uma categoria existente na lista."));
    }
}
