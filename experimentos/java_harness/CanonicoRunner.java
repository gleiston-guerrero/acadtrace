import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runner auxiliar del arnes experimentos/arnes_12_vectores.py.
 *
 * Lee por stdin un JSON {"vectores": [{"id": N, "contenido": {...}}, ...]},
 * invoca el metodo publico jsonCanonico(Map) de las dos clases reales
 * AuditHashService (sga-principal y secretaria, cargadas por FQN via
 * reflexion) y devuelve por stdout
 * {"principal": [...], "secretaria": [...]}.
 *
 * No contiene logica canonica propia: solo transporte (decodifica el
 * marcador {"$nan": true} al NaN nativo de Java) e invocacion.
 */
public class CanonicoRunner {

    private static final String FQN_PRINCIPAL =
            "ec.edu.uteq.sga.application.service.AuditHashService";
    private static final String FQN_SECRETARIA =
            "ec.uteq.sga.secretaria.application.service.AuditHashService";

    public static void main(String[] args) throws Exception {
        String entrada = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        ObjectMapper transporte = new ObjectMapper();

        @SuppressWarnings("unchecked")
        Map<String, Object> raiz = transporte.readValue(entrada, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vectores =
                (List<Map<String, Object>>) raiz.get("vectores");

        Object servicioPrincipal = Class.forName(FQN_PRINCIPAL)
                .getDeclaredConstructor().newInstance();
        Object servicioSecretaria = Class.forName(FQN_SECRETARIA)
                .getDeclaredConstructor().newInstance();
        Method canonicoPrincipal = Class.forName(FQN_PRINCIPAL)
                .getMethod("jsonCanonico", Map.class);
        Method canonicoSecretaria = Class.forName(FQN_SECRETARIA)
                .getMethod("jsonCanonico", Map.class);

        List<String> salidaPrincipal = new ArrayList<>();
        List<String> salidaSecretaria = new ArrayList<>();

        for (Map<String, Object> vector : vectores) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contenido =
                    (Map<String, Object>) decodificar(vector.get("contenido"));
            salidaPrincipal.add(invocar(canonicoPrincipal, servicioPrincipal, contenido));
            salidaSecretaria.add(invocar(canonicoSecretaria, servicioSecretaria, contenido));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("principal", salidaPrincipal);
        respuesta.put("secretaria", salidaSecretaria);
        System.out.print(transporte.writeValueAsString(respuesta));
    }

    private static String invocar(Method metodo, Object servicio,
            Map<String, Object> contenido) {
        try {
            return (String) metodo.invoke(servicio, contenido);
        } catch (Exception e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            return "ERROR:" + causa.getClass().getSimpleName()
                    + ":" + causa.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static Object decodificar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            if (mapa.size() == 1 && Boolean.TRUE.equals(mapa.get("$nan"))) {
                return Double.NaN;
            }
            Map<String, Object> resultado = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                resultado.put(String.valueOf(entrada.getKey()),
                        decodificar(entrada.getValue()));
            }
            return resultado;
        }
        if (valor instanceof List<?> lista) {
            List<Object> resultado = new ArrayList<>();
            for (Object elemento : lista) {
                resultado.add(decodificar(elemento));
            }
            return resultado;
        }
        return valor;
    }
}
