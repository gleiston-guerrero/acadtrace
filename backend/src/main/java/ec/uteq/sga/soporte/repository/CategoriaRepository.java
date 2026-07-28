package ec.uteq.sga.soporte.repository;

import ec.uteq.sga.soporte.model.Categoria;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends CrudRepository<Categoria, Long> {
    List<Categoria> findByActivoTrue();
}