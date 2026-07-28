package ec.uteq.sga.secretaria.security;

import ec.uteq.sga.secretaria.common.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Permite a los controllers recibir el usuario autenticado como parametro
 * (equivalente a leer req.user en Node), sin tocar HttpServletRequest directamente.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object user = webRequest.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (user == null) {
            throw new ApiException(401, "Token no proporcionado");
        }
        return user;
    }
}
