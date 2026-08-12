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


urlpatterns = [
    path("", api_root),
    path("admin/", admin.site.urls),
    path("api/docente/", include("docentes.urls")),
    path("", include("django_prometheus.urls")),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
