package ec.uteq.sga.secretaria.common;

import java.util.List;

/**
 * Equivalente a { data, meta: { total, page, limit, pages } } en los listados paginados de Node.
 */
public record PageResult<T>(List<T> data, Meta meta) {

    public record Meta(long total, int page, int limit, int pages) {}

    public static <T> PageResult<T> of(List<T> data, long total, int page, int limit) {
        int pages = limit > 0 ? (int) Math.ceil((double) total / limit) : 0;
        return new PageResult<>(data, new Meta(total, page, limit, pages));
    }
}
