from django.contrib import admin
from django.urls import include, path
from django.http import JsonResponse
from django.conf import settings
from django.conf.urls.static import static


def api_root(request):
    return JsonResponse({
        "status": "active",
        "service": "Microservicio Docente",
        "endpoints": {
            "admin": "/admin/",
            "api_docente": "/api/docente/"
        }
    })


def health_check(request):
    return JsonResponse({
        "status": "UP",
        "service": "Microservicio Docente"
    })


urlpatterns = [
    path("", api_root),
    path("health", health_check),
    path("health/", health_check),
    path("admin/", admin.site.urls),
    path("api/docente/", include("docentes.urls")),
    path("", include("django_prometheus.urls")),
]

<<<<<<< HEAD
=======
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
>>>>>>> de8866e6423acff01ffdc304ac29bc97ad14ff7d
