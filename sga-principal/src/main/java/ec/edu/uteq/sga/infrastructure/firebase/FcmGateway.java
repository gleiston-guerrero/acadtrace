package ec.edu.uteq.sga.infrastructure.firebase;
import java.util.Map;
public interface FcmGateway { enum Result { SENT, INVALID_TOKEN, FAILED, DISABLED } Result send(String token,String title,String body,Map<String,String> data); }
