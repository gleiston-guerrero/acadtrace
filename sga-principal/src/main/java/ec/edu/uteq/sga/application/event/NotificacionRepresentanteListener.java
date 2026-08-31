package ec.edu.uteq.sga.application.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Patrón GoF Observer: Escucha eventos de publicación de notas para
 * despachar notificaciones push a los representantes y registrar auditoría.
 */
@Component
public class NotificacionRepresentanteListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionRepresentanteListener.class);

    @EventListener
    public void onNotaPublicada(NotaPublicadaEvent event) {
        log.info("[OBSERVER] Evento recibido: Nueva nota registrada para el estudiante ID {} - Materia: {} - Nota: {} por Prof. {}",
                event.getIdEstudiante(), event.getMateria(), event.getCalificacion(), event.getDocente());
        
        // Simulación del despacho de notificación al representante
        despacharNotificacionMovil(event);
    }

    private void despacharNotificacionMovil(NotaPublicadaEvent event) {
        log.info("[OBSERVER-PUSH] Notificación enviada al canal móvil del representante del alumno {}",
                event.getCedulaEstudiante());
    }
}