from django.contrib import admin
from django.urls import include, path
from django.http import JsonResponse


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
]

