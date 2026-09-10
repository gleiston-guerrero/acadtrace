package ec.edu.uteq.sga.infrastructure.firebase;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
public class FirebaseAdminFcmGateway implements FcmGateway {
 private static final Logger log=LoggerFactory.getLogger(FirebaseAdminFcmGateway.class);
 private final boolean enabled;
 public FirebaseAdminFcmGateway(@Value("${app.firebase.enabled:false}") boolean enabled){this.enabled=enabled;}
 private FirebaseMessaging messaging() throws IOException {
   FirebaseApp app;
   if(FirebaseApp.getApps().isEmpty()) app=FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build());
   else app=FirebaseApp.getInstance();
   return FirebaseMessaging.getInstance(app);
 }
 public Result send(String token,String title,String body,Map<String,String> data){
   if(!enabled) return Result.DISABLED;
   try { messaging().send(Message.builder().setToken(token).setNotification(Notification.builder().setTitle(title).setBody(body).build()).putAllData(data).build()); return Result.SENT; }
   catch(FirebaseMessagingException e){
     if(e.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED || e.getMessagingErrorCode()==MessagingErrorCode.INVALID_ARGUMENT) return Result.INVALID_TOKEN;
     log.warn("FCM no pudo entregar una notificacion (codigo={})", e.getMessagingErrorCode()); return Result.FAILED;
   } catch(Exception e){ log.warn("FCM no disponible: {}", e.getClass().getSimpleName()); return Result.FAILED; }
 }
}
