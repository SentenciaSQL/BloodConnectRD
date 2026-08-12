package com.bloodconnect.common.util;

import com.bloodconnect.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableUtils {

    private PageableUtils() {
    }

    public static Pageable create(
            int page,
            int size,
            String sort,
            String direction,
            Set<String> allowedSorts,
            String defaultSort
    ) {
        if (page < 0) {
            throw new BadRequestException("El número de página no puede ser negativo");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("El tamaño de página debe estar entre 1 y 100");
        }
        String property = sort == null || sort.isBlank() ? defaultSort : sort;
        if (!allowedSorts.contains(property)) {
            throw new BadRequestException("El campo de ordenamiento no es válido");
        }
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction == null ? "desc" : direction);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("La dirección de ordenamiento debe ser asc o desc");
        }
        return PageRequest.of(page, size, Sort.by(sortDirection, property));
    }
}
