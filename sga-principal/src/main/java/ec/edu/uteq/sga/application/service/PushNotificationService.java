package ec.edu.uteq.sga.application.service;
import ec.edu.uteq.sga.domain.dto.notificacion.EventoAcademicoPushDTO;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.firebase.FcmGateway;
import ec.edu.uteq.sga.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class PushNotificationService {
 private final MatriculaRepository matriculas; private final AsignacionRepository asignaciones;
 private final DispositivoRepresentanteRepository dispositivos; private final EventoNotificacionPushRepository eventos; private final FcmGateway fcm;

 @Transactional public void procesar(EventoAcademicoPushDTO dto){
   if((dto.type().equals("AUSENTE")||dto.type().equals("ATRASO")) && dto.matriculaId()==null) return;
   usuariosDestino(dto).forEach(u->enviarUnaVez(dto.eventKey(),u,dto.type(),titulo(dto),cuerpo(dto),datos(dto,u)));
 }
 @Transactional public void cierre(PeriodoEvaluacion periodo){
   for(Matricula m: matriculas.findByAnoLectivoWithEstudiante(periodo.getAnoLectivo().getIdAnoLectivo())) {
     Usuario u=usuario(m); if(u==null) continue;
     String key="CIERRE_CALIFICACIONES:"+periodo.getIdPeriodo()+":"+m.getEstudiante().getIdEstudiante();
     enviarUnaVez(key,u,"CIERRE_CALIFICACIONES","Calificaciones disponibles","Las calificaciones del "+periodo.getNombre()+" ya estan disponibles.",
       Map.of("type","CIERRE_CALIFICACIONES","studentId",m.getEstudiante().getIdEstudiante().toString(),"periodId",periodo.getIdPeriodo().toString()));
   }
 }
 private Set<Usuario> usuariosDestino(EventoAcademicoPushDTO dto){
   Set<Usuario> out=new LinkedHashSet<>();
   if(dto.matriculaId()!=null) matriculas.findById(dto.matriculaId()).map(this::usuario).ifPresent(u->{if(u!=null)out.add(u);});
   if(dto.asignacionId()!=null) asignaciones.findById(dto.asignacionId()).ifPresent(a->matriculas.findByGrado_IdGradoAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivo(a.getGrado().getIdGrado(),a.getParalelo().getIdParalelo(),a.getAnoLectivo().getIdAnoLectivo()).stream().map(this::usuario).filter(Objects::nonNull).forEach(out::add));
   return out;
 }
 private Usuario usuario(Matricula m){return m.getEstudiante()!=null&&m.getEstudiante().getRepresentante()!=null?m.getEstudiante().getRepresentante().getUsuario():null;}
 private void enviarUnaVez(String key,Usuario u,String tipo,String title,String body,Map<String,String> data){
   EventoNotificacionPush ev=eventos.findByClaveEventoAndUsuario_IdUsuario(key,u.getIdUsuario()).orElseGet(()->eventos.save(EventoNotificacionPush.builder().claveEvento(key).usuario(u).tipo(tipo).build()));
   if("ENVIADO".equals(ev.getEstado())) return;
   boolean sent=false, failed=false;
   for(DispositivoRepresentante d:dispositivos.findByUsuario_IdUsuarioAndActivoTrue(u.getIdUsuario())){
     FcmGateway.Result r=fcm.send(d.getToken(),title,body,data);
     if(r==FcmGateway.Result.INVALID_TOKEN){d.setActivo(false);dispositivos.save(d);} else if(r==FcmGateway.Result.SENT) sent=true; else if(r==FcmGateway.Result.FAILED) failed=true;
   }
   ev.setEstado(sent?"ENVIADO":failed?"FALLIDO":"SIN_DISPOSITIVO"); if(sent)ev.setFechaEnvio(Instant.now()); eventos.save(ev);
 }
 private String titulo(EventoAcademicoPushDTO d){return d.title()!=null?d.title():switch(d.type()){case"COMUNICADO"->"Nuevo comunicado";case"AUSENTE"->"Ausencia registrada";default->"Atraso registrado";};}
 private String cuerpo(EventoAcademicoPushDTO d){return d.body()!=null?d.body():"Hay nueva informacion academica disponible.";}
 private Map<String,String> datos(EventoAcademicoPushDTO d,Usuario u){Map<String,String> m=new HashMap<>();m.put("type",d.type()); if(d.announcementId()!=null)m.put("announcementId",d.announcementId().toString()); if(d.periodId()!=null)m.put("periodId",d.periodId().toString()); if(d.matriculaId()!=null)matriculas.findById(d.matriculaId()).ifPresent(x->m.put("studentId",x.getEstudiante().getIdEstudiante().toString()));return m;}
}
