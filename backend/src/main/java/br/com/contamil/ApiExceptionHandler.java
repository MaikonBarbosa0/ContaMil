package br.com.contamil;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Registro não encontrado", exception.getMessage());
    }

    @ExceptionHandler(InvalidInputException.class)
    ProblemDetail invalidInput(InvalidInputException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        ProblemDetail result = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os campos informados.");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        result.setProperty("errors", errors);
        return result;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable() {
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Envie somente os campos aceitos, com datas no formato AAAA-MM-DD e valores numéricos com ponto decimal. Selecione a categoria pelo categoryId da lista.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail missingParameter(MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório",
                "Informe o parâmetro " + exception.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail invalidParameter(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Confira o valor de " + exception.getName() + ".");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict() {
        return problem(HttpStatus.CONFLICT, "Não foi possível salvar",
                "Os dados conflitam com um registro existente ou ultrapassam os limites permitidos. Atualize e tente novamente.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setTitle(title);
        return result;
    }
}
